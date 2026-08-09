package com.courseregistration.controller;

import com.courseregistration.dto.SideQuestRequest;
import com.courseregistration.dto.SideQuestResponse;
import com.courseregistration.dto.StudentQuestListResponse;
import com.courseregistration.security.AuthenticatedUser;
import com.courseregistration.service.SideQuestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SideQuestController {

    private final SideQuestService questService;

    public SideQuestController(SideQuestService questService) {
        this.questService = questService;
    }

    // ---------- Admin ----------

    @PostMapping("/api/admin/courses/{courseId}/quests")
    public SideQuestResponse create(@PathVariable Long courseId, @Valid @RequestBody SideQuestRequest req) {
        return questService.create(courseId, req);
    }

    @GetMapping("/api/admin/courses/{courseId}/quests")
    public List<SideQuestResponse> listForAdmin(@PathVariable Long courseId) {
        return questService.listForAdmin(courseId);
    }

    @DeleteMapping("/api/admin/quests/{questId}")
    public ResponseEntity<Void> delete(@PathVariable Long questId) {
        questService.delete(questId);
        return ResponseEntity.noContent().build();
    }

    // ---------- Student ----------

    @GetMapping("/api/student/courses/{courseId}/quests")
    public StudentQuestListResponse listForStudent(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable Long courseId) {
        return questService.listForStudent(user.userId(), courseId);
    }

    @PostMapping("/api/student/quests/{questId}/complete")
    public ResponseEntity<Void> complete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long questId) {
        questService.complete(user.userId(), questId);
        return ResponseEntity.noContent().build();
    }
}
