package edu.ntu.sc2002.application.contract;

import java.util.Objects;

public record TurnChoicesDTO(WaiterChoiceDTO waiterChoice, ChefChoiceDTO chefChoice) {
    public TurnChoicesDTO {
        Objects.requireNonNull(waiterChoice);
        Objects.requireNonNull(chefChoice);
        waiterChoice = new WaiterChoiceDTO(waiterChoice.taskType(), waiterChoice.targetCustomerId());
        chefChoice = new ChefChoiceDTO(chefChoice.taskType(), chefChoice.targetCustomerId());
    }
    public WaiterChoiceDTO getWaiterChoice() { return waiterChoice; }
    public ChefChoiceDTO getChefChoice() { return chefChoice; }
}
