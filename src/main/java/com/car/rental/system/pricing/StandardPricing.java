package com.car.rental.system.pricing;

import com.car.rental.system.model.Customer;
import com.car.rental.system.model.DateRange;
import com.car.rental.system.model.VehicleType;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * base daily rate x days
 *  + 20% surcharge on weekend days
 *  - 10% for rentals of 7+ days
 *  -  5% for loyalty members
 */
public class StandardPricing implements PricingStrategy {

    static final BigDecimal WEEKEND_SURCHARGE = new BigDecimal("0.20");
    static final BigDecimal WEEKLY_DISCOUNT = new BigDecimal("0.10");
    static final BigDecimal LOYALTY_DISCOUNT = new BigDecimal("0.05");

    @Override
    public BigDecimal price(VehicleType type, DateRange period, Customer customer) {
        BigDecimal rate = type.baseDailyRate();
        BigDecimal base = rate.multiply(BigDecimal.valueOf(period.days()));
        BigDecimal weekend = rate.multiply(WEEKEND_SURCHARGE).multiply(BigDecimal.valueOf(period.weekendDays()));
        BigDecimal total = base.add(weekend);

        if (period.days() >= 7) {
            total = total.subtract(total.multiply(WEEKLY_DISCOUNT));
        }
        if (customer.loyaltyMember()) {
            total = total.subtract(total.multiply(LOYALTY_DISCOUNT));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
