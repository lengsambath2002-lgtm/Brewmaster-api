package com.sambath.admincafe.order;

public enum OrderStatus {
    NEW,
    PREPARING,
    READY,
    PICKED_UP,
    COMPLETED,
    CANCELLED;

    public static OrderStatus fromDisplay(String value) {
        if (value == null) {
            throw new IllegalArgumentException("status is required");
        }
        return switch (value) {
            case "New" -> NEW;
            case "Preparing" -> PREPARING;
            case "Ready" -> READY;
            case "Picked Up" -> PICKED_UP;
            case "Completed" -> COMPLETED;
            case "Cancelled" -> CANCELLED;
            default -> throw new IllegalArgumentException("Invalid status: " + value);
        };
    }

    public String toDisplay() {
        return switch (this) {
            case NEW -> "New";
            case PREPARING -> "Preparing";
            case READY -> "Ready";
            case PICKED_UP -> "Picked Up";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }
}
