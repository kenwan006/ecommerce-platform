package com.example.sunridge.payment.model;

/** Stripe's authoritative result when Commerce tries to expire an incomplete checkout. */
public record CheckoutPaymentResolution(String outcome) {}
