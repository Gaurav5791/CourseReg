package com.courseregistration.service;

import com.courseregistration.dao.CourseDao;
import com.courseregistration.dao.EnrollmentDao;
import com.courseregistration.dao.QuizAttemptDao;
import com.courseregistration.dao.QuizDao;
import com.courseregistration.dao.QuizQuestionDao;
import com.courseregistration.dto.*;
import com.courseregistration.exception.ApiException;
import com.courseregistration.model.Quiz;
import com.courseregistration.model.QuizAttempt;
import com.courseregistration.model.QuizOption;
import com.courseregistration.model.QuizQuestion;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final QuizDao quizDao;
    private final QuizQuestionDao questionDao;
    private final QuizAttemptDao attemptDao;
    private final CourseDao courseDao;
    private final EnrollmentDao enrollmentDao;
    private final DataSource dataSource;

    public QuizService(QuizDao quizDao, QuizQuestionDao questionDao, QuizAttemptDao attemptDao,
                        CourseDao courseDao, EnrollmentDao enrollmentDao, DataSource dataSource) {
        this.quizDao = quizDao;
        this.questionDao = questionDao;
        this.attemptDao = attemptDao;
        this.courseDao = courseDao;
        this.enrollmentDao = enrollmentDao;
        this.dataSource = dataSource;
    }

    // ------------------------------------------------------------------
    // Admin
    // ------------------------------------------------------------------

    public QuizResponse createQuiz(Long courseId, QuizRequest req) {
        courseDao.findById(courseId).orElseThrow(() -> new ApiException(404, "Course not found"));
        Quiz quiz = quizDao.insert(courseId, req.title(), req.description());
        return new QuizResponse(quiz.id(), quiz.courseId(), quiz.title(), quiz.description(), 0);
    }

    public List<QuizResponse> listForAdmin(Long courseId) {
        return quizDao.findByCourse(courseId).stream()
                .map(q -> new QuizResponse(q.id(), q.courseId(), q.title(), q.description(),
                        questionDao.findByQuiz(q.id()).size()))
                .toList();
    }

    public QuizDetailResponse getDetailForAdmin(Long quizId) {
        Quiz quiz = quizDao.findById(quizId).orElseThrow(() -> new ApiException(404, "Quiz not found"));
        List<QuestionResponse> questions = questionDao.findByQuiz(quizId).stream()
                .map(q -> toQuestionResponse(q, true))
                .toList();
        return new QuizDetailResponse(quiz.id(), quiz.courseId(), quiz.title(), quiz.description(), questions);
    }

    public QuestionResponse addQuestion(Long quizId, QuestionRequest req) {
        quizDao.findById(quizId).orElseThrow(() -> new ApiException(404, "Quiz not found"));

        long correctCount = req.options().stream().filter(OptionRequest::correct).count();
        if (correctCount != 1) {
            throw new ApiException(400, "Exactly one option must be marked correct");
        }
        if (req.options().size() < 2) {
            throw new ApiException(400, "A question needs at least 2 options");
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long questionId = questionDao.insertQuestion(conn, quizId, req.questionText(), req.orderIndex());
                int order = 0;
                for (OptionRequest opt : req.options()) {
                    questionDao.insertOption(conn, questionId, opt.optionText(), opt.correct(), order++);
                }
                conn.commit();
                QuizQuestion saved = new QuizQuestion(questionId, quizId, req.questionText(), req.orderIndex());
                return toQuestionResponse(saved, true);
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                if (e instanceof RuntimeException re) throw re;
                throw new RuntimeException("Database error while adding question", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error opening transaction", e);
        }
    }

    public void deleteQuestion(Long questionId) {
        questionDao.deleteQuestion(questionId);
    }

    public void deleteQuiz(Long quizId) {
        quizDao.delete(quizId);
    }

    // ------------------------------------------------------------------
    // Student
    // ------------------------------------------------------------------

    public List<StudentQuizSummary> listForStudent(Long studentId, Long courseId) {
        requireApproved(studentId, courseId);
        return quizDao.findByCourse(courseId).stream().map(q -> {
            List<QuizAttempt> attempts = attemptDao.findByStudentAndQuiz(studentId, q.id());
            int questionCount = questionDao.findByQuiz(q.id()).size();
            Integer best = attempts.stream().map(QuizAttempt::score).max(Integer::compareTo).orElse(null);
            return new StudentQuizSummary(q.id(), q.courseId(), q.title(), q.description(),
                    questionCount, !attempts.isEmpty(), best);
        }).toList();
    }

    /** Questions + options WITHOUT the correct-answer flag — this is what the student sees while taking it. */
    public QuizDetailResponse getForTake(Long studentId, Long quizId) {
        Quiz quiz = quizDao.findById(quizId).orElseThrow(() -> new ApiException(404, "Quiz not found"));
        requireApproved(studentId, quiz.courseId());

        List<QuestionResponse> questions = questionDao.findByQuiz(quizId).stream()
                .map(q -> toQuestionResponse(q, false))
                .toList();
        return new QuizDetailResponse(quiz.id(), quiz.courseId(), quiz.title(), quiz.description(), questions);
    }

    public QuizAttemptResult submitAttempt(Long studentId, Long quizId, SubmitQuizRequest req) {
        Quiz quiz = quizDao.findById(quizId).orElseThrow(() -> new ApiException(404, "Quiz not found"));
        requireApproved(studentId, quiz.courseId());

        List<QuizQuestion> questions = questionDao.findByQuiz(quizId);
        if (questions.isEmpty()) {
            throw new ApiException(409, "This quiz has no questions yet");
        }

        // questionId -> submitted selectedOptionId (may be absent = skipped)
        Map<Long, Long> submitted = req.answers().stream()
                .collect(Collectors.toMap(AnswerSubmission::questionId, AnswerSubmission::selectedOptionId,
                        (a, b) -> b));

        List<QuestionResult> results = new ArrayList<>();
        int score = 0;

        for (QuizQuestion q : questions) {
            List<QuizOption> options = questionDao.findOptionsByQuestion(q.id());
            Set<Long> validOptionIds = options.stream().map(QuizOption::id).collect(Collectors.toSet());
            Long correctOptionId = options.stream().filter(QuizOption::correct).map(QuizOption::id)
                    .findFirst().orElse(null);

            Long selected = submitted.get(q.id());
            if (selected != null && !validOptionIds.contains(selected)) {
                throw new ApiException(400, "Invalid option submitted for question " + q.id());
            }

            boolean correct = selected != null && selected.equals(correctOptionId);
            if (correct) score++;

            results.add(new QuestionResult(q.id(), q.questionText(), selected, correctOptionId, correct));
        }

        final int finalScore = score;
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long attemptId = attemptDao.insertAttempt(conn, quizId, studentId, finalScore, questions.size());
                for (QuestionResult r : results) {
                    attemptDao.insertAnswer(conn, attemptId, r.questionId(), r.selectedOptionId(), r.wasCorrect());
                }
                conn.commit();
                var attempt = attemptDao.findById(attemptId).orElseThrow();
                return new QuizAttemptResult(attemptId, quizId, finalScore, questions.size(),
                        attempt.submittedAt().format(TS_FMT), results);
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                if (e instanceof RuntimeException re) throw re;
                throw new RuntimeException("Database error while submitting quiz", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error opening transaction", e);
        }
    }

    public List<QuizAttemptSummary> listMyAttempts(Long studentId, Long quizId) {
        quizDao.findById(quizId).orElseThrow(() -> new ApiException(404, "Quiz not found"));
        return attemptDao.findByStudentAndQuiz(studentId, quizId).stream()
                .map(a -> new QuizAttemptSummary(a.id(), a.score(), a.totalQuestions(), a.submittedAt().format(TS_FMT)))
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void requireApproved(Long studentId, Long courseId) {
        if (!enrollmentDao.isApprovedForCourse(studentId, courseId)) {
            throw new ApiException(403, "You need an approved enrollment in this course to access its quizzes");
        }
    }

    private QuestionResponse toQuestionResponse(QuizQuestion q, boolean revealCorrect) {
        List<OptionResponse> options = questionDao.findOptionsByQuestion(q.id()).stream()
                .map(o -> new OptionResponse(o.id(), o.optionText(), revealCorrect ? o.correct() : null))
                .toList();
        return new QuestionResponse(q.id(), q.questionText(), q.orderIndex(), options);
    }
}
