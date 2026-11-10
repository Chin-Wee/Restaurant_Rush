package edu.ntu.sc2002.application.contract;

import java.util.Objects;

/**
 * The Waiter command for one turn. Seating/order/serve commands address a customer; WAIT has no
 * target. The compact constructor validates that only WAIT may omit the target.
 *
 * @param taskType Waiter action
 * @param targetCustomerId selected customer ID, or null for WAIT
 */
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
