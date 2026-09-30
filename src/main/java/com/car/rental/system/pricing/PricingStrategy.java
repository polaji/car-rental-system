package com.car.rental.system.pricing;

import com.car.rental.system.model.Customer;
import com.car.rental.system.model.DateRange;
import com.car.rental.system.model.VehicleType;
import java.math.BigDecimal;

@FunctionalInterface
public interface PricingStrategy {
    BigDecimal price(VehicleType type, DateRange period, Customer customer);
}
