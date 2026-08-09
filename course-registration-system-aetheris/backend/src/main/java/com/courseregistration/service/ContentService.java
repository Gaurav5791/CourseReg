package com.courseregistration.service;

import com.courseregistration.dao.ContentProgressDao;
import com.courseregistration.dao.CourseContentDao;
import com.courseregistration.dao.CourseDao;
import com.courseregistration.dao.EnrollmentDao;
import com.courseregistration.dto.ContentRequest;
import com.courseregistration.dto.ContentResponse;
import com.courseregistration.exception.ApiException;
import com.courseregistration.model.ContentType;
import com.courseregistration.model.CourseContent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ContentService {

    private final CourseContentDao contentDao;
    private final CourseDao courseDao;
    private final EnrollmentDao enrollmentDao;
    private final ContentProgressDao progressDao;

    public ContentService(CourseContentDao contentDao, CourseDao courseDao, EnrollmentDao enrollmentDao,
                           ContentProgressDao progressDao) {
        this.contentDao = contentDao;
        this.courseDao = courseDao;
        this.enrollmentDao = enrollmentDao;
        this.progressDao = progressDao;
    }

    // ---------- Admin ----------

    public List<ContentResponse> listForAdmin(Long courseId) {
        requireCourseExists(courseId);
        return contentDao.findByCourse(courseId).stream().map(ContentResponse::from).toList();
    }

    public ContentResponse create(Long courseId, ContentRequest req) {
        requireCourseExists(courseId);
        ContentType type = parseType(req.contentType());
        validateBody(type, req);
        var saved = contentDao.insert(courseId, req.title(), type, req.body(), req.externalUrl(), req.orderIndex());
        return ContentResponse.from(saved);
    }

    public ContentResponse update(Long contentId, ContentRequest req) {
        ContentType type = parseType(req.contentType());
        validateBody(type, req);
        var saved = contentDao.update(contentId, req.title(), type, req.body(), req.externalUrl(), req.orderIndex());
        return ContentResponse.from(saved);
    }

    public void delete(Long contentId) {
        contentDao.delete(contentId);
    }

    // ---------- Student ----------

    /** Only students with an APPROVED enrollment can see a course's lessons. */
    public List<ContentResponse> listForStudent(Long studentId, Long courseId) {
        requireCourseExists(courseId);
        requireApproved(studentId, courseId);

        Set<Long> completedIds = progressDao.findCompletedContentIds(studentId);
        return contentDao.findByCourse(courseId).stream()
                .map(c -> ContentResponse.forStudent(c, completedIds.contains(c.id())))
                .toList();
    }

    public void markComplete(Long studentId, Long contentId) {
        CourseContent content = contentDao.findById(contentId)
                .orElseThrow(() -> new ApiException(404, "Content item not found"));
        requireApproved(studentId, content.courseId());
        progressDao.markComplete(studentId, contentId);
    }

    // ---------- Helpers ----------

    private void requireCourseExists(Long courseId) {
        courseDao.findById(courseId).orElseThrow(() -> new ApiException(404, "Course not found"));
    }

    private void requireApproved(Long studentId, Long courseId) {
        if (!enrollmentDao.isApprovedForCourse(studentId, courseId)) {
            throw new ApiException(403, "You need an approved enrollment in this course to do that");
        }
    }

    private ContentType parseType(String raw) {
        try {
            return ContentType.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw new ApiException(400, "contentType must be TEXT or LINK");
        }
    }

    private void validateBody(ContentType type, ContentRequest req) {
        if (type == ContentType.TEXT && (req.body() == null || req.body().isBlank())) {
            throw new ApiException(400, "body is required for TEXT content");
        }
        if (type == ContentType.LINK && (req.externalUrl() == null || req.externalUrl().isBlank())) {
            throw new ApiException(400, "externalUrl is required for LINK content");
        }
    }
}
