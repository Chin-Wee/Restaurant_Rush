package edu.ntu.sc2002.application.contract;

import java.util.Objects;

/**
 * The Chef command for one turn. ADVANCE addresses the customer who owns the order; WAIT has no
 * target. The compact constructor validates the target/task combination.
 *
 * @param taskType Chef action
 * @param targetCustomerId selected customer ID, or null for WAIT
 */
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
