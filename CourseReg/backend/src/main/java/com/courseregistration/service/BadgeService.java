package com.courseregistration.service;

import com.courseregistration.dao.CertificateDao;
import com.courseregistration.dao.EnrollmentDao;
import com.courseregistration.dao.QuizAttemptDao;
import com.courseregistration.dao.QuizDao;
import com.courseregistration.dto.Badge;
import com.courseregistration.model.Enrollment;
import com.courseregistration.model.EnrollmentStatus;
import com.courseregistration.model.Quiz;
import com.courseregistration.model.QuizAttempt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Engagement badges — deliberately built with zero new tables. Everything
 * here is derived from data the app already tracks (lesson completion,
 * quiz attempts, certificates), so there's no separate "quest" system to
 * maintain in parallel with the real progress data.
 */
@Service
public class BadgeService {

    private final EnrollmentDao enrollmentDao;
    private final ProgressService progressService;
    private final QuizDao quizDao;
    private final QuizAttemptDao attemptDao;
    private final CertificateDao certificateDao;

    public BadgeService(EnrollmentDao enrollmentDao, ProgressService progressService,
                         QuizDao quizDao, QuizAttemptDao attemptDao, CertificateDao certificateDao) {
        this.enrollmentDao = enrollmentDao;
        this.progressService = progressService;
        this.quizDao = quizDao;
        this.attemptDao = attemptDao;
        this.certificateDao = certificateDao;
    }

    public List<Badge> getMyBadges(Long studentId) {
        List<Enrollment> approved = enrollmentDao.findByStudent(studentId).stream()
                .filter(e -> e.status() == EnrollmentStatus.APPROVED)
                .toList();

        int totalLessonsCompleted = 0;
        boolean anyQuizPerfect = false;

        for (Enrollment e : approved) {
            var progress = progressService.computeProgress(studentId, e.courseId());
            totalLessonsCompleted += progress.lessonsCompleted();

            for (Quiz quiz : quizDao.findByCourse(e.courseId())) {
                for (QuizAttempt attempt : attemptDao.findByStudentAndQuiz(studentId, quiz.id())) {
                    if (attempt.totalQuestions() > 0 && attempt.score() == attempt.totalQuestions()) {
                        anyQuizPerfect = true;
                    }
                }
            }
        }

        int certificatesEarned = certificateDao.findByStudent(studentId).size();

        List<Badge> badges = new ArrayList<>();
        if (totalLessonsCompleted >= 1) {
            badges.add(new Badge("first_steps", "First Steps", "Completed your first lesson", "\uD83D\uDC63"));
        }
        if (totalLessonsCompleted >= 10) {
            badges.add(new Badge("dedicated_learner", "Dedicated Learner", "Completed 10+ lessons", "\uD83D\uDCDA"));
        }
        if (anyQuizPerfect) {
            badges.add(new Badge("quiz_ace", "Quiz Ace", "Scored 100% on a quiz", "\uD83C\uDFAF"));
        }
        if (certificatesEarned >= 1) {
            badges.add(new Badge("course_complete", "Course Complete", "Earned your first certificate", "\uD83C\uDF93"));
        }
        if (certificatesEarned >= 2) {
            badges.add(new Badge("multi_talented", "Multi-Talented", "Earned certificates in 2+ courses", "\uD83C\uDF1F"));
        }

        return badges;
    }
}
