package com.dacare.server.service;

import com.dacare.server.domain.AppUser;
import com.dacare.server.domain.Customer;
import com.dacare.server.domain.Engineer;
import com.dacare.server.domain.Reservation;
import com.dacare.server.repository.AppUserRepository;
import com.dacare.server.repository.CustomerRepository;
import com.dacare.server.repository.EngineerRepository;
import com.dacare.server.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

  private final AppUserRepository users;
  private final CustomerRepository customers;
  private final EngineerRepository engineers;
  private final ReservationRepository reservations;
  private final ApplicationEventPublisher events;
  private final PasswordEncoder encoder;

  public ReservationService(AppUserRepository users, CustomerRepository customers,
      EngineerRepository engineers, ReservationRepository reservations,
      ApplicationEventPublisher events, PasswordEncoder encoder) {
    this.users = users;
    this.customers = customers;
    this.engineers = engineers;
    this.reservations = reservations;
    this.events = events;
    this.encoder = encoder;
  }

  @Transactional
  public Reservation create(String email, String deviceType, String symptom, String address,
      LocalDateTime preferredAt, String contactName, String contactPhone) {
      if (!preferredAt.isAfter(LocalDateTime.now())) {
          throw new IllegalArgumentException("희망 방문 일시는 미래여야 합니다.");
      }
    AppUser user = users.findByEmail(email)
        .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
    Customer customer = customers.findByUser(user)
        .orElseThrow(() -> new NoSuchElementException("고객 정보를 찾을 수 없습니다."));
    Reservation reservation = new Reservation(customer, deviceType, symptom, address, preferredAt);
    reservation.setContact(contactName == null ? customer.getName() : contactName,
        contactPhone == null ? customer.getPhone() : contactPhone);
    Reservation saved = reservations.save(reservation);
    events.publishEvent(new ReservationReceivedEvent(saved.getId()));
    return saved;
  }

  @Transactional
  public Reservation createGuest(String deviceType, String symptom, String address,
      LocalDateTime preferredAt, String contactName, String contactPhone, String guestPassword) {
      if (!preferredAt.isAfter(LocalDateTime.now())) {
          throw new IllegalArgumentException("희망 방문 일시는 미래여야 합니다.");
      }
    String passwordHash =
        (guestPassword == null || guestPassword.isBlank()) ? null : encoder.encode(guestPassword);
    Reservation reservation = new Reservation(deviceType, symptom, address, preferredAt,
        contactName, contactPhone, passwordHash);
    Reservation saved = reservations.save(reservation);
    events.publishEvent(new ReservationReceivedEvent(saved.getId()));
    return saved;
  }

  public Reservation findGuest(Long id, String contactPhone, String guestPassword) {
    Reservation reservation = reservations.findById(id)
        .orElseThrow(() -> new NoSuchElementException("예약을 찾을 수 없습니다."));
      if (!reservation.isGuest()) {
          throw new NoSuchElementException("예약을 찾을 수 없습니다.");
      }
      if (reservation.getContactPhone() == null || !reservation.getContactPhone().replace("-", "")
          .equals(contactPhone.replace("-", ""))) {
          throw new NoSuchElementException("예약을 찾을 수 없습니다.");
      }
    if (reservation.getGuestPasswordHash() != null) {
        if (guestPassword == null || !encoder.matches(guestPassword,
            reservation.getGuestPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }
    }
    return reservation;
  }

  @Transactional
  public Reservation cancelGuest(Long id, String contactPhone, String guestPassword) {
    Reservation reservation = findGuest(id, contactPhone, guestPassword);
    reservation.cancel();
    return reservation;
  }

  public List<Reservation> mine(String email) {
    AppUser user = users.findByEmail(email)
        .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
    return reservations.findAllByCustomerOrderByCreatedAtDesc(customers.findByUser(user)
        .orElseThrow(() -> new NoSuchElementException("고객 정보를 찾을 수 없습니다.")));
  }

  public Reservation mineOne(String email, Long id) {
    Reservation reservation = reservations.findById(id)
        .orElseThrow(() -> new NoSuchElementException("예약을 찾을 수 없습니다."));
    AppUser user = users.findByEmail(email)
        .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
    Customer customer = customers.findByUser(user)
        .orElseThrow(() -> new NoSuchElementException("고객 정보를 찾을 수 없습니다."));
      if (reservation.getCustomer() == null || !reservation.getCustomer().getId()
          .equals(customer.getId())) {
          throw new NoSuchElementException("예약을 찾을 수 없습니다.");
      }
    return reservation;
  }

  @Transactional
  public Reservation cancel(String email, Long id) {
    Reservation reservation = mineOne(email, id);
    reservation.cancel();
    return reservation;
  }

  public List<Reservation> all() {
    return reservations.findAll();
  }

  @Transactional
  public Reservation confirm(Long reservationId, Long engineerId, LocalDateTime confirmedAt) {
      if (!confirmedAt.isAfter(LocalDateTime.now())) {
          throw new IllegalArgumentException("확정 방문 일시는 미래여야 합니다.");
      }
    Reservation reservation = reservations.findById(reservationId)
        .orElseThrow(() -> new NoSuchElementException("예약을 찾을 수 없습니다."));
    Engineer engineer = engineers.findById(engineerId)
        .orElseThrow(() -> new NoSuchElementException("기사를 찾을 수 없습니다."));
    reservation.confirm(engineer, confirmedAt);
    events.publishEvent(new ReservationConfirmedEvent(reservation.getId()));
    return reservation;
  }

  @Transactional
  public Reservation complete(Long reservationId) {
    Reservation reservation = reservations.findById(reservationId)
        .orElseThrow(() -> new NoSuchElementException("예약을 찾을 수 없습니다."));
    reservation.complete();
    return reservation;
  }

  @Transactional
  public Reservation cancelByAdmin(Long reservationId) {
    Reservation reservation = reservations.findById(reservationId)
        .orElseThrow(() -> new NoSuchElementException("예약을 찾을 수 없습니다."));
    reservation.cancelByAdmin();
    return reservation;
  }
}

record ReservationConfirmedEvent(Long reservationId) {

}

record ReservationReceivedEvent(Long reservationId) {

}
