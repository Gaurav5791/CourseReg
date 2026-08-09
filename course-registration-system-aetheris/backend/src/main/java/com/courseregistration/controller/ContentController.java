package com.courseregistration.controller;

import com.courseregistration.dto.ContentRequest;
import com.courseregistration.dto.ContentResponse;
import com.courseregistration.security.AuthenticatedUser;
import com.courseregistration.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    // ---------- Admin: manage a course's lessons ----------
    // These sit under /api/admin/** so the existing SecurityConfig rule
    // (hasRole("ADMIN")) already covers them — nothing to add there.

    @GetMapping("/api/admin/courses/{courseId}/contents")
    public List<ContentResponse> listForAdmin(@PathVariable Long courseId) {
        return contentService.listForAdmin(courseId);
    }

    @PostMapping("/api/admin/courses/{courseId}/contents")
    public ContentResponse create(@PathVariable Long courseId, @Valid @RequestBody ContentRequest req) {
        return contentService.create(courseId, req);
    }

    @PutMapping("/api/admin/contents/{id}")
    public ContentResponse update(@PathVariable Long id, @Valid @RequestBody ContentRequest req) {
        return contentService.update(id, req);
    }

    @DeleteMapping("/api/admin/contents/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Student: view a course's lessons (approved enrollment only) ----------

    @GetMapping("/api/student/courses/{courseId}/contents")
    public List<ContentResponse> listForStudent(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable Long courseId) {
        return contentService.listForStudent(user.userId(), courseId);
    }

    @PostMapping("/api/student/contents/{contentId}/complete")
    public ResponseEntity<Void> markComplete(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable Long contentId) {
        contentService.markComplete(user.userId(), contentId);
        return ResponseEntity.noContent().build();
    }
}
