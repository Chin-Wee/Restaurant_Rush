package edu.ntu.sc2002.application.contract;
/** Lifecycle phase of a turn, used by the controller and UI to reject commands out of order. */
public enum TurnPhase { START_OF_TURN, MANAGER_CHOICES, RESOLVING, GAME_OVER }
