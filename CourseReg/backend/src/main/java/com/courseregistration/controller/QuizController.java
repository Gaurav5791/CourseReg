package com.courseregistration.controller;

import com.courseregistration.dto.*;
import com.courseregistration.security.AuthenticatedUser;
import com.courseregistration.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // ---------- Admin ----------

    @PostMapping("/api/admin/courses/{courseId}/quizzes")
    public QuizResponse createQuiz(@PathVariable Long courseId, @Valid @RequestBody QuizRequest req) {
        return quizService.createQuiz(courseId, req);
    }

    @GetMapping("/api/admin/courses/{courseId}/quizzes")
    public List<QuizResponse> listForAdmin(@PathVariable Long courseId) {
        return quizService.listForAdmin(courseId);
    }

    @GetMapping("/api/admin/quizzes/{quizId}")
    public QuizDetailResponse getDetailForAdmin(@PathVariable Long quizId) {
        return quizService.getDetailForAdmin(quizId);
    }

    @PostMapping("/api/admin/quizzes/{quizId}/questions")
    public QuestionResponse addQuestion(@PathVariable Long quizId, @Valid @RequestBody QuestionRequest req) {
        return quizService.addQuestion(quizId, req);
    }

    @DeleteMapping("/api/admin/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long questionId) {
        quizService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/admin/quizzes/{quizId}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long quizId) {
        quizService.deleteQuiz(quizId);
        return ResponseEntity.noContent().build();
    }

    // ---------- Student ----------

    @GetMapping("/api/student/courses/{courseId}/quizzes")
    public List<StudentQuizSummary> listForStudent(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @PathVariable Long courseId) {
        return quizService.listForStudent(user.userId(), courseId);
    }

    @GetMapping("/api/student/quizzes/{quizId}/take")
    public QuizDetailResponse getForTake(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long quizId) {
        return quizService.getForTake(user.userId(), quizId);
    }

    @PostMapping("/api/student/quizzes/{quizId}/submit")
    public QuizAttemptResult submitAttempt(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable Long quizId,
                                            @Valid @RequestBody SubmitQuizRequest req) {
        return quizService.submitAttempt(user.userId(), quizId, req);
    }

    @GetMapping("/api/student/quizzes/{quizId}/attempts")
    public List<QuizAttemptSummary> listMyAttempts(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable Long quizId) {
        return quizService.listMyAttempts(user.userId(), quizId);
    }
}
