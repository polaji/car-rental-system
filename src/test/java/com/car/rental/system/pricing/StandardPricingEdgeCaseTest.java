package com.car.rental.system.pricing;

import com.car.rental.system.model.Customer;
import com.car.rental.system.model.DateRange;
import com.car.rental.system.model.VehicleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardPricingEdgeCaseTest {

    final PricingStrategy pricing = new StandardPricing();
    final Customer regular = new Customer("C1", "Reg", "DL1", false);
    final Customer loyal = new Customer("C2", "Loyal", "DL2", true);
    final LocalDate monday = LocalDate.of(2026, 9, 28);
    final LocalDate saturday = LocalDate.of(2026, 10, 3);

    // Hand-calculated table. weekday rate r; weekend day = 1.2r; >=7 days -10%; loyalty -5% (applied after weekly).
    @ParameterizedTest(name = "{0} from {1} for {2} days, loyal={3} -> {4}")
    @CsvSource({
            "SEDAN, 2026-09-28, 1, false, 45.00",    // single weekday
            "VAN,   2026-09-28, 1, false, 85.00",
            "SUV,   2026-10-03, 1, false, 72.00",    // Saturday: 60 * 1.2
            "SEDAN, 2026-09-28, 6, false, 279.00",   // 6 days = NO weekly discount: 270 + 1 Sat x 9
            "SEDAN, 2026-09-28, 7, false, 299.70",   // 7 days = weekly discount kicks in: (315+18) * 0.9
            "VAN,   2026-09-28, 3, true,  242.25",   // loyalty only: 255 * 0.95
            "VAN,   2026-09-28, 14, true, 1075.59"}) // both discounts: (1190 + 4 x 17) * 0.9 * 0.95
    void priceTable(VehicleType type, LocalDate start, int days, boolean loyalty, String expected) {
        Customer c = loyalty ? loyal : regular;
        assertEquals(new BigDecimal(expected), pricing.price(type, DateRange.of(start, days), c));
    }

    @Test
    void sevenDayBoundaryIsInclusive() {
        BigDecimal six = pricing.price(VehicleType.SEDAN, DateRange.of(monday, 6), regular);
        BigDecimal seven = pricing.price(VehicleType.SEDAN, DateRange.of(monday, 7), regular);
        assertTrue(seven.compareTo(six) > 0, "7th day still costs something");
        assertTrue(seven.compareTo(new BigDecimal("315.00").add(new BigDecimal("18.00"))) < 0, "discount applied at 7");
    }

    @Test
    void resultAlwaysHasTwoDecimalPlaces() {
        for (VehicleType t : VehicleType.values()) {
            for (int days = 1; days <= 15; days++) {
                assertEquals(2, pricing.price(t, DateRange.of(saturday, days), loyal).scale(), t + " " + days + "d");
            }
        }
    }

    @Test
    void longerRentalAlwaysCostsMore_evenAcrossTheWeeklyDiscountCliff() {
        // Property: a 7-day rental (>= 6.66 x rate) must never be cheaper than a 6-day one (<= 6.4 x rate),
        // otherwise customers would book 7 days and return early. Check every start weekday and every type.
        for (VehicleType t : VehicleType.values()) {
            for (int offset = 0; offset < 7; offset++) {
                LocalDate start = monday.plusDays(offset);
                BigDecimal prev = BigDecimal.ZERO;
                for (int days = 1; days <= 30; days++) {
                    BigDecimal p = pricing.price(t, DateRange.of(start, days), regular);
                    assertTrue(p.compareTo(prev) > 0, t + " from " + start.getDayOfWeek() + ", " + days + "d");
                    prev = p;
                }
            }
        }
    }

    @Test
    void pricingIsAFunctionalInterface_soPromotionsCanBeComposed() {
        PricingStrategy tenOff = (type, period, customer) -> pricing.price(type, period, customer).subtract(BigDecimal.TEN);
        assertEquals(new BigDecimal("35.00"), tenOff.price(VehicleType.SEDAN, DateRange.of(monday, 1), regular));
    }
}

