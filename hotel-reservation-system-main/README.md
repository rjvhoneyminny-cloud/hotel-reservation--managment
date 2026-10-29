# Hotel Room Reservation System

A Java 17 desktop application built with Swing, JDBC and MySQL. It includes room and guest CRUD, reservation booking/cancellation, overlap checks, CSV export and a Java serialization backup. Maven manages the MySQL JDBC driver.

## Requirements

- JDK 17 or newer (`java -version` and `javac -version`)
- Maven 3.9 or newer (`mvn -version`)
- MySQL Server running locally(I prefer installing xampp server)
- VS Code with the **Extension Pack for Java** (recommended)

## Database setup

1. Open `src/main/resources/schema.sql` in MySQL Workbench or a MySQL terminal and execute it. This creates `hotel_reservation` and its three tables. The app inserts three sample rooms the first time it starts.
2. Configure connection settings as environment variables before launching. Defaults are URL `jdbc:mysql://localhost:3306/hotel_reservation?serverTimezone=UTC`, user `root`, and an empty password.

PowerShell example (change credentials to match your MySQL account):

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/hotel_reservation?serverTimezone=UTC"
$env:DB_USER = "root"
$env:DB_PASSWORD = "your_mysql_password"
```

Do not commit real database passwords. `.env.example` documents the variable names; Java does not automatically load `.env` files.

## Open and run in VS Code

1. Open the `hotel-reservation-system` folder in VS Code.
2. Wait for Maven dependency import to finish.
3. Set the database environment variables in the VS Code terminal using the example above.
4. Run `mvn compile exec:java` from the project folder. Maven downloads MySQL Connector/J on the first run.

Alternatively, use the Java Projects view and run `hotel.HotelApp`. The app connects to MySQL during startup, so execute the schema first.

## Features and implementation notes

- **Rooms:** create, retrieve, edit and delete rooms. Rooms with reservation history cannot be deleted; mark them inactive instead.
- **Guests:** create, retrieve, edit and delete guests. Existing reservations prevent deletion.
- **Reservations:** book an active room for a guest, inspect reservations, and cancel a booking. Check-out must be after check-in.
- **Overlap rule:** two bookings conflict when `existing.check_in < requested.check_out AND existing.check_out > requested.check_in`. Thus, same-day check-out/check-in is allowed. Cancelled reservations do not block a room.
- **Collections:** Swing table models hold display rows; `HashMap` indexes loaded rooms, `ArrayList` holds guests, and `LinkedList` holds reservations.
- **File handling:** export the currently loaded reservation list as CSV.
- **Serialization:** save reservation records to `hotel-backup.ser` and load a selected backup for in-memory review. Restore does not overwrite the database.
- **Validation and errors:** required fields, email shape, positive capacity, non-negative rate, date order, overlap and SQL constraints are checked; database and file errors are shown in dialogs.

## Project layout

```text
hotel-reservation-system/
├── pom.xml
├── .env.example
├── README.md
└── src/main/
    ├── java/hotel/
    │   ├── Db.java
    │   ├── HotelApp.java
    │   └── Models.java
    └── resources/schema.sql
```

## Demo flow

1. Start MySQL, execute the schema, configure credentials and launch the app.
2. Add a guest, then book an active sample room for a date range.
3. Try a second booking for the same room with dates that overlap; the app rejects it. Try a booking beginning on the first booking's check-out date; it is accepted.
4. Update a room or guest, then cancel a reservation and confirm that the cancelled booking no longer blocks dates.
5. Export the reservations to CSV and save/load a serialized backup.

## Troubleshooting

- **Communications link failure:** start MySQL and confirm `DB_URL` host/port.
- **Access denied:** set `DB_USER` and `DB_PASSWORD` to a valid MySQL account.
- **Unknown database/table:** execute `schema.sql` before launching.
- **Maven cannot download Connector/J:** check internet/proxy access and retry Maven dependency import.
- **Duplicate email/room number:** choose a unique value; the database enforces both constraints.

## Known scope

This is a course-project desktop app intended for a single operator. It does not implement authentication, payment, tax/invoice processing, or concurrent booking transactions across multiple users. The availability check and insert are separate operations, so a production multi-user system should wrap them in a transaction and enforce concurrency-safe booking logic.
