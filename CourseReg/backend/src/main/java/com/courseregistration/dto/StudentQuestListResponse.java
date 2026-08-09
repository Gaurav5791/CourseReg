package com.courseregistration.dto;

import java.util.List;

public record StudentQuestListResponse(
        List<SideQuestResponse> quests,
        int totalPointsEarned
) {}
