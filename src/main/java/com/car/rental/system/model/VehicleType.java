package com.car.rental.system.model;

import java.math.BigDecimal;

public enum VehicleType {
    SEDAN(new BigDecimal("45.00")),
    SUV(new BigDecimal("60.00")),
    VAN(new BigDecimal("85.00"));

    private final BigDecimal baseDailyRate;

    VehicleType(BigDecimal baseDailyRate) {
        this.baseDailyRate = baseDailyRate;
    }

    public BigDecimal baseDailyRate() {
        return baseDailyRate;
    }
}
