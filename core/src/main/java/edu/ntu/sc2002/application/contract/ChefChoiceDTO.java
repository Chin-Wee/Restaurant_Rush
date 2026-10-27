package edu.ntu.sc2002.application.contract;

/** Cooking targets a domain order ID; waiting uses zero. */
public record ChefChoiceDTO(boolean isCooking, int targetOrderId) {
    public ChefChoiceDTO {
        if (isCooking ? targetOrderId < 1 : targetOrderId != 0) throw new IllegalArgumentException("Invalid Chef target");
    }
    public int getTargetOrderId() { return targetOrderId; }
}
