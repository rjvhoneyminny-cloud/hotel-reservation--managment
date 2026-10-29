CREATE DATABASE IF NOT EXISTS hotel_reservation CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hotel_reservation;
CREATE TABLE IF NOT EXISTS rooms (
  id INT AUTO_INCREMENT PRIMARY KEY,
  room_number VARCHAR(12) NOT NULL UNIQUE,
  room_type VARCHAR(40) NOT NULL,
  capacity INT NOT NULL CHECK (capacity > 0),
  nightly_rate DECIMAL(10,2) NOT NULL CHECK (nightly_rate >= 0),
  active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS guests (
  id INT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL,
  phone VARCHAR(30) NOT NULL,
  UNIQUE KEY uq_guest_email (email)
);
CREATE TABLE IF NOT EXISTS reservations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  room_id INT NOT NULL,
  guest_id INT NOT NULL,
  check_in DATE NOT NULL,
  check_out DATE NOT NULL,
  status ENUM('BOOKED','CANCELLED') NOT NULL DEFAULT 'BOOKED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_res_room FOREIGN KEY (room_id) REFERENCES rooms(id),
  CONSTRAINT fk_res_guest FOREIGN KEY (guest_id) REFERENCES guests(id),
  CONSTRAINT ck_dates CHECK (check_out > check_in),
  INDEX ix_res_room_dates (room_id, status, check_in, check_out)
);
