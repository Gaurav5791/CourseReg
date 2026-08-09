package com.courseregistration.service;

import com.courseregistration.dao.CourseDao;
import com.courseregistration.dto.CourseRequest;
import com.courseregistration.dto.CourseResponse;
import com.courseregistration.exception.ApiException;
import com.courseregistration.model.Course;
import com.courseregistration.model.CourseStatus;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class CourseService {

    private final CourseDao courseDao;

    public CourseService(CourseDao courseDao) {
        this.courseDao = courseDao;
    }

    public List<CourseResponse> browseActive(String keyword, String semester) {
        return courseDao.findAllActive(keyword, semester).stream()
                .map(CourseResponse::from)
                .toList();
    }

    /** Used by admin/registrar views, which need to see removed courses too. */
    public List<CourseResponse> listAll() {
        return courseDao.findAll().stream().map(CourseResponse::from).toList();
    }

    public CourseResponse getById(Long id) {
        Course course = courseDao.findById(id)
                .orElseThrow(() -> new ApiException(404, "Course not found"));
        return CourseResponse.from(course);
    }

    public CourseResponse create(CourseRequest req) {
        Course toInsert = new Course(
                null, req.code(), req.title(), req.description(), req.credits(),
                req.instructorName(), req.dayOfWeek(),
                parseTime(req.startTime(), "startTime"), parseTime(req.endTime(), "endTime"),
                req.semester(), req.capacity(), 0, CourseStatus.ACTIVE
        );
        validateTimes(toInsert);
        return CourseResponse.from(courseDao.insert(toInsert));
    }

    public CourseResponse update(Long id, CourseRequest req) {
        Course existing = courseDao.findById(id)
                .orElseThrow(() -> new ApiException(404, "Course not found"));

        if (req.capacity() < existing.seatsTaken()) {
            throw new ApiException(409, "Capacity can't be lower than the " + existing.seatsTaken() + " seats already taken");
        }

        Course updated = new Course(
                id, existing.code(), req.title(), req.description(), req.credits(),
                req.instructorName(), req.dayOfWeek(),
                parseTime(req.startTime(), "startTime"), parseTime(req.endTime(), "endTime"),
                req.semester(), req.capacity(), existing.seatsTaken(), existing.status()
        );
        validateTimes(updated);
        return CourseResponse.from(courseDao.update(id, updated));
    }

    private void validateTimes(Course c) {
        if (!c.endTime().isAfter(c.startTime())) {
            throw new ApiException(400, "endTime must be after startTime");
        }
    }

    private LocalTime parseTime(String raw, String field) {
        try {
            return raw.length() == 5
                    ? LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm"))
                    : LocalTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw new ApiException(400, field + " must look like \"HH:mm\", e.g. \"09:30\"");
        }
    }
}
