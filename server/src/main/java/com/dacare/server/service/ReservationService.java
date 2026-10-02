package com.dacare.server.service;

import com.dacare.server.domain.Customer;
import com.dacare.server.domain.Engineer;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.domain.Reservation;
import com.dacare.server.repository.AppUserRepository;
import com.dacare.server.repository.CustomerRepository;
import com.dacare.server.repository.EngineerRepository;
import com.dacare.server.repository.ReservationRepository;
import com.dacare.server.service.event.ReservationConfirmedEvent;
import com.dacare.server.service.event.ReservationReceivedEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationService {

  private static final String RESERVATION_NOT_FOUND = "예약을 찾을 수 없습니다.";
  private static final String GUEST_RESERVATION_NOT_FOUND =
      "예약 정보를 찾을 수 없습니다. 예약 번호, 휴대전화 번호, 비밀번호를 확인해 주세요.";

  private final AppUserRepository users;
  private final CustomerRepository customers;
  private final EngineerRepository engineers;
  private final ReservationRepository reservations;
  private final ApplicationEventPublisher events;
  private final PasswordEncoder encoder;
  private final GuestLookupThrottle throttle;
  private final Clock clock;

  public ReservationService(AppUserRepository users, CustomerRepository customers,
      EngineerRepository engineers, ReservationRepository reservations,
      ApplicationEventPublisher events, PasswordEncoder encoder, GuestLookupThrottle throttle,
      Clock clock) {
    this.users = users;
    this.customers = customers;
    this.engineers = engineers;
    this.reservations = reservations;
    this.events = events;
    this.encoder = encoder;
    this.throttle = throttle;
    this.clock = clock;
  }

  @Transactional
  public Reservation create(String email, ReservationDraft draft) {
    requireFuture(draft.preferredAt(), "희망 방문 일시는 미래여야 합니다.");
    Customer customer = getCustomer(email);
    Reservation reservation = new Reservation(customer, draft.deviceType(),
        draft.symptomDescription(), draft.visitAddress(), draft.preferredAt());
    reservation.setContact(Objects.requireNonNullElse(draft.contactName(), customer.getName()),
        Objects.requireNonNullElse(draft.contactPhone(), customer.getPhone()));
    return receive(reservation);
  }

  @Transactional
  public Reservation createGuest(ReservationDraft draft, String guestPassword) {
    requireFuture(draft.preferredAt(), "희망 방문 일시는 미래여야 합니다.");
    return receive(new Reservation(draft.deviceType(), draft.symptomDescription(),
        draft.visitAddress(), draft.preferredAt(), draft.contactName(), draft.contactPhone(),
        encoder.encode(guestPassword)));
  }

  /**
   * @param clientKey 비밀번호 대입 시도를 IP별로 세기 위한 요청 IP의 해시
   */
  public Reservation findGuest(Long id, PhoneNumber contactPhone, String guestPassword,
      String clientKey) {
    throttle.check(id, clientKey);
    Reservation reservation = reservations.findById(id)
        .filter(found -> matchesGuest(found, contactPhone, guestPassword))
        .orElseThrow(() -> {
          throttle.recordFailure(id, clientKey);
          return new NoSuchElementException(GUEST_RESERVATION_NOT_FOUND);
        });
    throttle.reset(id);
    return reservation;
  }

  @Transactional
  public Reservation cancelGuest(Long id, PhoneNumber contactPhone, String guestPassword,
      String clientKey) {
    Reservation reservation = findGuest(id, contactPhone, guestPassword, clientKey);
    reservation.cancel();
    return reservation;
  }

  public List<Reservation> mine(String email) {
    return reservations.findAllByCustomerOrderByCreatedAtDesc(getCustomer(email));
  }

  public Reservation mineOne(String email, Long id) {
    Reservation reservation = getReservation(id);
    Customer customer = getCustomer(email);
    if (reservation.getCustomer() == null
        || !reservation.getCustomer().getId().equals(customer.getId())) {
      throw new NoSuchElementException(RESERVATION_NOT_FOUND);
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
    return reservations.findAllByOrderByCreatedAtDesc();
  }

  @Transactional
  public Reservation confirm(Long reservationId, Long engineerId, LocalDateTime confirmedAt) {
    requireFuture(confirmedAt, "확정 방문 일시는 미래여야 합니다.");
    Reservation reservation = getReservation(reservationId);
    Engineer engineer = engineers.findById(engineerId)
        .orElseThrow(() -> new NoSuchElementException("기사를 찾을 수 없습니다."));
    reservation.confirm(engineer, confirmedAt);
    events.publishEvent(new ReservationConfirmedEvent(reservation.getId()));
    return reservation;
  }

  @Transactional
  public Reservation complete(Long reservationId) {
    Reservation reservation = getReservation(reservationId);
    reservation.complete();
    return reservation;
  }

  @Transactional
  public Reservation cancelByAdmin(Long reservationId) {
    Reservation reservation = getReservation(reservationId);
    reservation.cancelByAdmin();
    return reservation;
  }

  private Reservation receive(Reservation reservation) {
    Reservation saved = reservations.save(reservation);
    events.publishEvent(new ReservationReceivedEvent(saved.getId()));
    return saved;
  }

  /**
   * 어떤 항목이 틀렸는지 드러나지 않도록 회원 예약, 연락처 불일치, 비밀번호 불일치를 구분하지 않는다. 비밀번호 없이 접수된 예전 예약은
   * 예약 번호와 연락처만으로 열리지 않도록 조회 대상에서 뺀다.
   */
  private boolean matchesGuest(Reservation reservation, PhoneNumber contactPhone,
      String guestPassword) {
    String passwordHash = reservation.getGuestPasswordHash();
    return reservation.isGuest() && contactPhone.equals(reservation.getContactPhone())
        && passwordHash != null && encoder.matches(guestPassword, passwordHash);
  }

  private Reservation getReservation(Long id) {
    return reservations.findById(id)
        .orElseThrow(() -> new NoSuchElementException(RESERVATION_NOT_FOUND));
  }

  private Customer getCustomer(String email) {
    return users.findByEmail(email).flatMap(customers::findByUser)
        .orElseThrow(() -> new NoSuchElementException("고객 정보를 찾을 수 없습니다."));
  }

  /**
   * 일시는 한국 시간 기준 LocalDateTime이므로 서버 시스템 시간대가 아닌 주입된 Clock으로 비교한다.
   */
  private void requireFuture(LocalDateTime dateTime, String message) {
    if (!dateTime.isAfter(LocalDateTime.now(clock))) {
      throw new IllegalArgumentException(message);
    }
  }
}
