package edu.ntu.sc2002.application.contract;

import java.util.Objects;

/** The manager's Host task; WAIT has no target customer. */
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
