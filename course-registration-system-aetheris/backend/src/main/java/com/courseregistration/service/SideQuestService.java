package com.courseregistration.service;

import com.courseregistration.dao.CourseDao;
import com.courseregistration.dao.EnrollmentDao;
import com.courseregistration.dao.SideQuestDao;
import com.courseregistration.dto.SideQuestRequest;
import com.courseregistration.dto.SideQuestResponse;
import com.courseregistration.dto.StudentQuestListResponse;
import com.courseregistration.exception.ApiException;
import com.courseregistration.model.SideQuest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class SideQuestService {

    private final SideQuestDao questDao;
    private final CourseDao courseDao;
    private final EnrollmentDao enrollmentDao;

    public SideQuestService(SideQuestDao questDao, CourseDao courseDao, EnrollmentDao enrollmentDao) {
        this.questDao = questDao;
        this.courseDao = courseDao;
        this.enrollmentDao = enrollmentDao;
    }

    // ---------- Admin ----------

    public SideQuestResponse create(Long courseId, SideQuestRequest req) {
        courseDao.findById(courseId).orElseThrow(() -> new ApiException(404, "Course not found"));
        SideQuest saved = questDao.insert(courseId, req.title(), req.description(), req.points());
        return toResponse(saved, null);
    }

    public List<SideQuestResponse> listForAdmin(Long courseId) {
        return questDao.findByCourse(courseId).stream().map(q -> toResponse(q, null)).toList();
    }

    public void delete(Long questId) {
        questDao.delete(questId);
    }

    // ---------- Student ----------

    public StudentQuestListResponse listForStudent(Long studentId, Long courseId) {
        requireApproved(studentId, courseId);

        Set<Long> completed = questDao.findCompletedQuestIds(studentId);
        List<SideQuestResponse> quests = questDao.findByCourse(courseId).stream()
                .map(q -> toResponse(q, completed.contains(q.id())))
                .toList();
        int totalPoints = questDao.sumPointsForCourse(studentId, courseId);

        return new StudentQuestListResponse(quests, totalPoints);
    }

    public void complete(Long studentId, Long questId) {
        SideQuest quest = questDao.findById(questId).orElseThrow(() -> new ApiException(404, "Quest not found"));
        requireApproved(studentId, quest.courseId());
        questDao.markComplete(studentId, questId);
    }

    private void requireApproved(Long studentId, Long courseId) {
        if (!enrollmentDao.isApprovedForCourse(studentId, courseId)) {
            throw new ApiException(403, "You need an approved enrollment in this course to do that");
        }
    }

    private SideQuestResponse toResponse(SideQuest q, Boolean completed) {
        return new SideQuestResponse(q.id(), q.courseId(), q.title(), q.description(), q.points(), completed);
    }
}
