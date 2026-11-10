package edu.ntu.sc2002.application.contract;

import java.util.Objects;

public record TurnChoicesDTO(WaiterChoiceDTO waiterChoice, ChefChoiceDTO chefChoice,
        HostChoiceDTO hostChoice, boolean activateHappyHour) {
    public TurnChoicesDTO {
        Objects.requireNonNull(waiterChoice);
        Objects.requireNonNull(chefChoice);
        Objects.requireNonNull(hostChoice);
        waiterChoice = new WaiterChoiceDTO(waiterChoice.taskType(), waiterChoice.targetCustomerId());
        chefChoice = new ChefChoiceDTO(chefChoice.taskType(), chefChoice.targetCustomerId());
        hostChoice = new HostChoiceDTO(hostChoice.taskType(), hostChoice.targetCustomerId());
    }
    public WaiterChoiceDTO getWaiterChoice() { return waiterChoice; }
    public ChefChoiceDTO getChefChoice() { return chefChoice; }
    public HostChoiceDTO getHostChoice() { return hostChoice; }
    public boolean isActivateHappyHourRequested() { return activateHappyHour; }
}
