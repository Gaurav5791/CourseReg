package com.courseregistration.dto;

/** Badges are computed on the fly from existing progress data — there's no "badges" table. */
public record Badge(
        String id,
        String title,
        String description,
        String icon
) {}
