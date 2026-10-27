package edu.ntu.sc2002.application.contract;

import java.util.Objects;

public record TurnChoicesDTO(WaiterChoiceDTO waiterChoice, ChefChoiceDTO chefChoice) {
    public TurnChoicesDTO {
        Objects.requireNonNull(waiterChoice);
        Objects.requireNonNull(chefChoice);
    }
    public WaiterChoiceDTO getWaiterChoice() { return waiterChoice; }
    public ChefChoiceDTO getChefChoice() { return chefChoice; }
}
