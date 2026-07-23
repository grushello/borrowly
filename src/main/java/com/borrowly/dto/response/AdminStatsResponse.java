package com.borrowly.dto.response;

public record AdminStatsResponse(
        long totalUsers,
        long adminUsers,
        long disabledUsers,
        long totalItems,
        long activeItems,
        long rentedItems,
        long archivedItems,
        long totalCategories
) {
}
