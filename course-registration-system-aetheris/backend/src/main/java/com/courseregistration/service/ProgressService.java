package com.courseregistration.service;

import com.courseregistration.dao.ContentProgressDao;
import com.courseregistration.dao.CourseContentDao;
import com.courseregistration.dao.QuizAttemptDao;
import com.courseregistration.dao.QuizDao;
import com.courseregistration.dto.CourseProgressResponse;
import com.courseregistration.model.Quiz;
import com.courseregistration.model.QuizAttempt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Computes "how far along is this student in this course" — the one place
 * that decides what "complete" means, so the admin analytics view and the
 * certificate eligibility check can never disagree with each other.
 */
@Service
public class ProgressService {

    /** A quiz counts as "passed" at 60% or better. Change this one constant to adjust the bar everywhere. */
    private static final double PASS_THRESHOLD_PERCENT = 60.0;

    private final CourseContentDao contentDao;
    private final ContentProgressDao progressDao;
    private final QuizDao quizDao;
    private final QuizAttemptDao attemptDao;

    public ProgressService(CourseContentDao contentDao, ContentProgressDao progressDao,
                            QuizDao quizDao, QuizAttemptDao attemptDao) {
        this.contentDao = contentDao;
        this.progressDao = progressDao;
        this.quizDao = quizDao;
        this.attemptDao = attemptDao;
    }

    public CourseProgressResponse computeProgress(Long studentId, Long courseId) {
        int totalLessons = contentDao.findByCourse(courseId).size();
        int lessonsCompleted = progressDao.countCompletedForCourse(studentId, courseId);

        List<Quiz> quizzes = quizDao.findByCourse(courseId);
        int totalQuizzes = quizzes.size();
        int quizzesAttempted = 0;
        int quizzesPassed = 0;
        double percentSum = 0;

        for (Quiz quiz : quizzes) {
            List<QuizAttempt> attempts = attemptDao.findByStudentAndQuiz(studentId, quiz.id());
            if (attempts.isEmpty()) continue;

            quizzesAttempted++;
            QuizAttempt best = attempts.stream()
                    .max((a, b) -> Integer.compare(a.score(), b.score()))
                    .orElseThrow();
            double percent = best.totalQuestions() == 0 ? 0 : (100.0 * best.score() / best.totalQuestions());
            percentSum += percent;
            if (percent >= PASS_THRESHOLD_PERCENT) quizzesPassed++;
        }

        Double avgQuizPercent = quizzesAttempted == 0 ? null : percentSum / quizzesAttempted;

        boolean hasAnyWork = totalLessons > 0 || totalQuizzes > 0;
        boolean lessonsDone = totalLessons == 0 || lessonsCompleted == totalLessons;
        boolean quizzesDone = totalQuizzes == 0 || quizzesPassed == totalQuizzes;
        boolean eligible = hasAnyWork && lessonsDone && quizzesDone;

        return new CourseProgressResponse(lessonsCompleted, totalLessons, quizzesAttempted,
                quizzesPassed, totalQuizzes, avgQuizPercent, eligible);
    }
}
