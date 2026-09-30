## Car Rental Service

A small, well-tested booking engine for a car rental company, written in plain Java.

---

## Features

- Search for available cars by type and dates
- Get a price quote before booking
- Reserve, pick up, return, and cancel bookings
- Customer booking history, sorted by date
- Revenue report by car type
- Safe under concurrent bookings (many users booking at the same moment)

---

## Business rules

| Rule | Detail |
|---|---|
| **Dates** | Like a hotel stay: the pickup day counts, the return day doesn't. Oct 5 → Oct 8 is **3 days**, and the car can go out again on Oct 8. |
| **Base daily rate** | SEDAN $45 · SUV $60 · VAN $85 |
| **Weekend surcharge** | +20% for each Saturday or Sunday rented |
| **Weekly discount** | −10% for rentals of 7 days or more |
| **Loyalty discount** | −5% for loyalty members (applied after the weekly discount) |
| **Rounding** | Once, at the end, to 2 decimal places |
| **Booking lifecycle** | `RESERVED → ACTIVE → COMPLETED`, or `RESERVED → CANCELLED` |
| **Car lifecycle** | `AVAILABLE, RENTED`, or `MAINTENANCE` |

---

## Project structure

```
src/main/java/com/car/rental/system
├── model/          Domain objects and their rules (depends on nothing else)
│   ├── Vehicle.java          a car: plate, type, mileage, status
│   ├── Reservation.java      a booking and its lifecycle
│   ├── Customer.java         a renter
│   ├── DateRange.java        [start, end) period, overlap and weekend logic
│   └── VehicleType.java      SEDAN / SUV / VAN and base rates
├── pricing/        Pricing rules (Strategy pattern)
│   ├── PricingStrategy.java
│   └── StandardPricing.java
├── repository/     Storage interfaces + in-memory implementations
└── service/        RentalService: the use cases, plus exceptions

src/test/java/...   99 JUnit 5 tests
```

---

## Requirements

- **Java 8** or newer (JDK 9+ recommended for building, since the compiler targets Java 8)
- **Maven 3.6+**

---

## Build and test

mvn clean install

Run a single test class:
mvn -Dtest=RentalServiceScenarioTest test


---

## Quick example

```java
VehicleRepository fleet = new InMemoryVehicleRepository();
fleet.save(new Vehicle("ABC-123", "Toyota", "Camry", VehicleType.SEDAN, 1000));

RentalService service = new RentalService(
        fleet, new InMemoryReservationRepository(), new StandardPricing());

Customer alan = new Customer("C1", "Alan", "DL-001", false);
DateRange trip = DateRange.of(LocalDate.of(2026, 10, 5), 3);   // Oct 5 → Oct 8

BigDecimal price = service.quote(VehicleType.SEDAN, trip, alan);  // 135.00
Reservation r    = service.reserve(alan, VehicleType.SEDAN, trip);

service.pickUp(r.id());                 // car → RENTED,    booking → ACTIVE
service.returnVehicle(r.id(), 1250);    // car → AVAILABLE, booking → COMPLETED
```

---

## Errors

| Exception | When |
|---|---|
| 'NoAvailabilityException' | No car of that type is free for those dates |
| 'NotFoundException' | Unknown reservation id |
| 'IllegalStateException' | An illegal move, e.g. cancelling an active rental or picking up a cancelled one |
| 'IllegalArgumentException' | Bad input, e.g. end date before start, missing license, odometer going backwards |

---

## Design highlights

- the 'model' package is at the centre and depends only on standard Java. Service, pricing and storage depend on it, never the other way round.
- the 'reserve()' locks one car at a time, so "is it free?" and "book it" can't be split by another thread, while bookings for different cars still run in parallel.
- pick-up and return check the booking's status before touching the car, so a failure never leaves things half-updated.
- new promotions are a new 'PricingStrategy', with no change to booking code.
- 'BigDecimal' throughout.

---

## Tests

99 JUnit 5 tests, including:

- every allowed and forbidden booking status move
- date edge cases (year end, leap years, weekends, back-to-back bookings)
- hand-calculated price tables, plus a check that longer rentals always cost more
- concurrency tests with 32 threads booking at once

---

## Known limitations

- Storage is in memory only, so data is lost on restart.
- The per-car lock works within a single server. Running several servers needs a database-level guard (for example a Postgres exclusion constraint).
- Maintenance isn't tied to dates, and pick-up doesn't check the booking's start date.
- No late-return fees yet.


