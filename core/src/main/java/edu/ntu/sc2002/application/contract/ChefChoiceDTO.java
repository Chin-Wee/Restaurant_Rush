package edu.ntu.sc2002.application.contract;

import java.util.Objects;

/** Cooking targets a domain order ID; waiting uses zero. */
public record ChefChoiceDTO(ChefTaskType taskType, int targetOrderId) {
    public ChefChoiceDTO {
        Objects.requireNonNull(taskType);
        if (taskType == ChefTaskType.COOK ? targetOrderId < 1 : targetOrderId != 0) throw new IllegalArgumentException("Invalid Chef target");
    }
    public ChefTaskType getTaskType() { return taskType; }
    public int getTargetOrderId() { return targetOrderId; }
}
