package com.courseregistration.dto;

import com.courseregistration.model.CourseContent;

public record ContentResponse(
        Long id,
        Long courseId,
        String title,
        String contentType,
        String body,
        String externalUrl,
        int orderIndex,
        Boolean completed   // null for the admin view; true/false when returned to a student
) {
    /** Admin view — completion doesn't apply here. */
    public static ContentResponse from(CourseContent c) {
        return new ContentResponse(
                c.id(), c.courseId(), c.title(), c.contentType().name(),
                c.body(), c.externalUrl(), c.orderIndex(), null
        );
    }

    /** Student view — tells the frontend whether to show a checkmark. */
    public static ContentResponse forStudent(CourseContent c, boolean completed) {
        return new ContentResponse(
                c.id(), c.courseId(), c.title(), c.contentType().name(),
                c.body(), c.externalUrl(), c.orderIndex(), completed
        );
    }
}
