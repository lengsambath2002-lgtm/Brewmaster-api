package com.sambath.admincafe.report;

public enum ReportRange {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public static ReportRange parse(String value, ReportRange fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return switch (value.toLowerCase()) {
            case "daily" -> DAILY;
            case "weekly" -> WEEKLY;
            case "monthly" -> MONTHLY;
            case "yearly" -> YEARLY;
            default -> throw new IllegalArgumentException("Invalid range: " + value);
        };
    }

    public String displayName() {
        return name().toLowerCase();
    }
}
