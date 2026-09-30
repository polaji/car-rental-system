package com.car.rental.system.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class CustomerTest {

    @Test
    void validCustomerIsCreated() {
        Customer c = new Customer("C1", "Customer1", "DL-123", true);
        assertEquals("C1", c.id());
        assertTrue(c.loyaltyMember());
    }

    @Test
    void idAndNameAreMandatory() {
        assertThrows(NullPointerException.class, () -> new Customer(null, "Customer1", "DL1", false));
        assertThrows(NullPointerException.class, () -> new Customer("C1", null, "DL1", false));
    }

    @Test
    void recordsHaveValueEquality() {
        assertEquals(new Customer("C1", "Customer1", "DL1", false),
                new Customer("C1", "Customer1", "DL1", false));
    }
}
