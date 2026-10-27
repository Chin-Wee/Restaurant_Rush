package edu.ntu.sc2002.application.contract;

import java.util.Objects;

/** Target is the customer's arrival ordinal for every task, or zero for WAIT. */
public record WaiterChoiceDTO(WaiterTaskType taskType, int targetId) {
    public WaiterChoiceDTO {
        Objects.requireNonNull(taskType);
        if (taskType == WaiterTaskType.WAIT ? targetId != 0 : targetId < 1) throw new IllegalArgumentException("Invalid Waiter target");
    }
    public WaiterTaskType getTaskType() { return taskType; }
    public int getTargetId() { return targetId; }
}
