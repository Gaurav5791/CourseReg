package com.courseregistration.model;

import java.time.LocalTime;

public record Course(
        Long id,
        String code,
        String title,
        String description,
        int credits,
        String instructorName,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String semester,
        int capacity,
        int seatsTaken,
        CourseStatus status
) {
    public int seatsAvailable() {
        return capacity - seatsTaken;
    }
}
