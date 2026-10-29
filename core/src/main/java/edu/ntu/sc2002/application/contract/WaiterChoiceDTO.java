package edu.ntu.sc2002.application.contract;

import java.util.Objects;

/** Commands address the customer who owns an order; WAIT has no target. */
public record WaiterChoiceDTO(WaiterTaskType taskType, String targetCustomerId) {
    public WaiterChoiceDTO {
        Objects.requireNonNull(taskType);
        if (taskType == WaiterTaskType.WAIT ? targetCustomerId != null : targetCustomerId == null || targetCustomerId.isBlank()) {
            throw new IllegalArgumentException("Invalid Waiter target");
        }
    }
    public WaiterTaskType getTaskType() { return taskType; }
    public String getTargetCustomerId() { return targetCustomerId; }
}
