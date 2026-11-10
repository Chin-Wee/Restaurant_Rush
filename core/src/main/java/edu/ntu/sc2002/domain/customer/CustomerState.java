package edu.ntu.sc2002.domain.customer;

/** Customer lifecycle from waiting for a table through payment or abandonment. */
public enum CustomerState { WAITING, SEATED, EATING, READY_TO_PAY, PAID, LEFT }
