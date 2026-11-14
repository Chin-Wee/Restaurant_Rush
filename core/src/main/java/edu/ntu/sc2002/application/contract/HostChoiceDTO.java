package edu.ntu.sc2002.application.contract;

import java.util.Objects;

/**
 * The manager's Host command for one turn. SEAT addresses one queued customer; WAIT has no target.
 *
 * @param taskType Host action
 * @param targetCustomerId selected customer ID, or null for WAIT
 */
public record HostChoiceDTO(HostTaskType taskType, String targetCustomerId) {
    public HostChoiceDTO {
        Objects.requireNonNull(taskType);
        if (taskType == HostTaskType.WAIT ? targetCustomerId != null : targetCustomerId == null || targetCustomerId.isBlank()) {
            throw new IllegalArgumentException("Invalid Host target");
        }
    }
    public HostTaskType getTaskType() { return taskType; }
    public String getTargetCustomerId() { return targetCustomerId; }
}
