package com.courseregistration.dto;

import com.courseregistration.model.Course;

public record CourseResponse(
        Long id,
        String code,
        String title,
        String description,
        int credits,
        String instructorName,
        String dayOfWeek,
        String startTime,
        String endTime,
        String semester,
        int capacity,
        int seatsTaken,
        int seatsAvailable,
        String status
) {
    public static CourseResponse from(Course c) {
        return new CourseResponse(
                c.id(), c.code(), c.title(), c.description(), c.credits(),
                c.instructorName(), c.dayOfWeek(),
                c.startTime().toString(), c.endTime().toString(),
                c.semester(), c.capacity(), c.seatsTaken(), c.seatsAvailable(),
                c.status().name()
        );
    }
}
