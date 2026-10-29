package hotel;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

record Room(int id, String number, String type, int capacity, BigDecimal rate, boolean active) {
    @Override public String toString() { return number + " — " + type + " ($" + rate + ")"; }
}
record Guest(int id, String name, String email, String phone) {
    @Override public String toString() { return name + " — " + email; }
}
record Reservation(int id, String room, String guest, LocalDate checkIn, LocalDate checkOut, String status) implements Serializable {
    private static final long serialVersionUID = 1L;
}
