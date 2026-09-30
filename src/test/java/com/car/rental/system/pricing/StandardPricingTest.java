package com.car.rental.system.pricing;

import com.car.rental.system.model.Customer;
import com.car.rental.system.model.DateRange;
import com.car.rental.system.model.VehicleType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardPricingTest {

    final PricingStrategy pricing = new StandardPricing();
    final Customer regular = new Customer("C1", "Reg", "DL1", false);
    final Customer loyal = new Customer("C2", "Loyal", "DL2", true);
    final LocalDate monday = LocalDate.of(2026, 9, 28);

    @Test
    void weekdaysOnly_isRateTimesDays() {
        // Mon..Thu (3 days), ECONOMY 45 -> 135.00
        assertEquals(new BigDecimal("135.00"), pricing.price(VehicleType.SEDAN, DateRange.of(monday, 3), regular));
    }

    @Test
    void weekendDays_addTwentyPercent() {
        // Fri, Sat, Sun: 3 x 45 = 135 + 2 x 9 = 153.00
        assertEquals(new BigDecimal("153.00"), pricing.price(VehicleType.SEDAN, DateRange.of(monday.plusDays(4), 3), regular));
    }

    @Test
    void weekOrLonger_getsTenPercentOff() {
        // 7 days from Monday: 7 x 60 = 420 + 2 weekend x 12 = 444 -> -10% = 399.60
        assertEquals(new BigDecimal("399.60"), pricing.price(VehicleType.SUV, DateRange.of(monday, 7), regular));
    }

    @Test
    void loyaltyDiscountStacksAfterWeeklyDiscount() {
        // 399.60 - 5% = 379.62
        assertEquals(new BigDecimal("379.62"), pricing.price(VehicleType.SUV, DateRange.of(monday, 7), loyal));
    }
}

