package edu.ntu.sc2002.domain;

/** Order lifecycle from placement through preparation, service, payment or cancellation. */
public enum OrderStatus { PLACED, PREPARING, READY, SERVED, PAID, CANCELLED }
