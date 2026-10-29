package edu.ntu.sc2002.application.contract;

import java.util.Objects;

/** Commands address the customer who owns an order; WAIT has no target. */
public record ChefChoiceDTO(ChefTaskType taskType, String targetCustomerId) {
    public ChefChoiceDTO {
        Objects.requireNonNull(taskType);
        if (taskType == ChefTaskType.WAIT ? targetCustomerId != null : targetCustomerId == null || targetCustomerId.isBlank()) {
            throw new IllegalArgumentException("Invalid Chef target");
        }
    }
    public ChefTaskType getTaskType() { return taskType; }
    public String getTargetCustomerId() { return targetCustomerId; }
}
