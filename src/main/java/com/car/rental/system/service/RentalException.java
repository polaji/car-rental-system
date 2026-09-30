package com.car.rental.system.service;

public class RentalException extends RuntimeException {
    public RentalException(String message) {
        super(message);
    }

    public static class NoAvailabilityException extends RentalException {
        public NoAvailabilityException(String message) { super(message); }
    }

    public static class NotFoundException extends RentalException {
        public NotFoundException(String message) { super(message); }
    }
}
