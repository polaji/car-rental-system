package com.car.rental.system.model;

import java.util.Objects;

/**
 * A renter. Immutable value object: final class, final fields, no setters, equality by value.
 */
public final class Customer {

    private final String id;
    private final String name;
    private final String licenseNumber;
    private final boolean loyaltyMember;

    public Customer(String id, String name, String licenseNumber, boolean loyaltyMember) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (licenseNumber == null || licenseNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("driver's license required");
        }
        this.id = id;
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.loyaltyMember = loyaltyMember;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String licenseNumber() {
        return licenseNumber;
    }

    public boolean loyaltyMember() {
        return loyaltyMember;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer other = (Customer) o;
        return loyaltyMember == other.loyaltyMember
                && id.equals(other.id)
                && name.equals(other.name)
                && licenseNumber.equals(other.licenseNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, licenseNumber, loyaltyMember);
    }

    @Override
    public String toString() {
        return "Customer[id=" + id + ", name=" + name + ", licenseNumber=" + licenseNumber
                + ", loyaltyMember=" + loyaltyMember + "]";
    }
}
