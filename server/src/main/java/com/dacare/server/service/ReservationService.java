package com.dacare.server.service;

import com.dacare.server.domain.Customer;
import com.dacare.server.domain.Engineer;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.domain.Reservation;
import com.dacare.server.domain.ReservationCode;
import com.dacare.server.repository.AppUserRepository;
import com.dacare.server.repository.CustomerRepository;
import com.dacare.server.repository.EngineerRepository;
import com.dacare.server.repository.ReservationRepository;
import com.dacare.server.service.event.ReservationConfirmedEvent;
import com.dacare.server.service.event.ReservationReceivedEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationService {

  private static final String RESERVATION_NOT_FOUND = "예약을 찾을 수 없습니다.";
  private static final String GUEST_RESERVATION_NOT_FOUND =
      "예약 정보를 찾을 수 없습니다. 예약 번호와 휴대전화 번호를 확인해 주세요.";
  /**
   * 비회원 예약을 완료·취소 후에도 조회할 수 있는 기간. 해지된 번호가 다른 사람에게 다시 배정되는 경우 등을 고려해 지난 예약이 계속
   * 열려 있지 않게 한다. 전체 이력은 회원의 '내 예약'에서 제공한다.
   */
  static final Duration GUEST_LOOKUP_PERIOD = Duration.ofDays(90);

  private final AppUserRepository users;
  private final CustomerRepository customers;
  private final EngineerRepository engineers;
  private final ReservationRepository reservations;
  private final ApplicationEventPublisher events;
  private final GuestLookupThrottle throttle;
  private final Clock clock;

  public ReservationService(AppUserRepository users, CustomerRepository customers,
      EngineerRepository engineers, ReservationRepository reservations,
      ApplicationEventPublisher events, GuestLookupThrottle throttle, Clock clock) {
    this.users = users;
    this.customers = customers;
    this.engineers = engineers;
    this.reservations = reservations;
    this.events = events;
    this.throttle = throttle;
    this.clock = clock;
  }

  @Transactional
  public Reservation create(String email, ReservationDraft draft) {
    requireFuture(draft.preferredAt(), "희망 방문 일시는 미래여야 합니다.");
    Customer customer = getCustomer(email);
    Reservation reservation = new Reservation(newCode(), customer, draft.deviceType(),
        draft.symptomDescription(), draft.visitAddress(), draft.preferredAt());
    reservation.setContact(Objects.requireNonNullElse(draft.contactName(), customer.getName()),
        Objects.requireNonNullElse(draft.contactPhone(), customer.getPhone()));
    return receive(reservation);
  }

  @Transactional
  public Reservation createGuest(ReservationDraft draft) {
    requireFuture(draft.preferredAt(), "희망 방문 일시는 미래여야 합니다.");
    return receive(new Reservation(newCode(), draft.deviceType(), draft.symptomDescription(),
        draft.visitAddress(), draft.preferredAt(), draft.contactName(), draft.contactPhone()));
  }

  /**
   * 예약 번호와 휴대전화 번호가 모두 맞는 비회원 예약 한 건을 찾는다.
   *
   * @param clientKey 대입 시도를 IP별로 세기 위한 요청 IP의 해시
   */
  public Reservation findGuest(String code, PhoneNumber contactPhone, String clientKey) {
    // 형식이 틀린 입력도 같은 실패로 세어 응답만으로 형식 검사 결과가 드러나지 않게 한다.
    String normalized = ReservationCode.normalize(code).orElse(code);
    throttle.check(normalized, contactPhone, clientKey);
    Reservation reservation = reservations.findByCode(normalized)
        .filter(found -> matchesGuest(found, contactPhone))
        .orElseThrow(() -> {
          throttle.recordFailure(normalized, contactPhone, clientKey);
          return new NoSuchElementException(GUEST_RESERVATION_NOT_FOUND);
        });
    throttle.reset(normalized, contactPhone);
    return reservation;
  }

  @Transactional
  public Reservation cancelGuest(String code, PhoneNumber contactPhone, String clientKey) {
    Reservation reservation = findGuest(code, contactPhone, clientKey);
    reservation.cancel(LocalDateTime.now(clock));
    return reservation;
  }

  public List<Reservation> mine(String email) {
    return reservations.findAllByCustomerOrderByCreatedAtDesc(getCustomer(email));
  }

  public Reservation mineOne(String email, Long id) {
    return requireOwner(email, getReservation(id));
  }

  /**
   * @param code 고객이 알고 있는 예약 번호(예: 4821-7390)
   */
  public Reservation mineOneByCode(String email, String code) {
    Reservation reservation = ReservationCode.normalize(code).flatMap(reservations::findByCode)
        .orElseThrow(() -> new NoSuchElementException(RESERVATION_NOT_FOUND));
    return requireOwner(email, reservation);
  }

  private Reservation requireOwner(String email, Reservation reservation) {
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
    reservation.cancel(LocalDateTime.now(clock));
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
    reservation.complete(LocalDateTime.now(clock));
    return reservation;
  }

  @Transactional
  public Reservation cancelByAdmin(Long reservationId) {
    Reservation reservation = getReservation(reservationId);
    reservation.cancelByAdmin(LocalDateTime.now(clock));
    return reservation;
  }

  private Reservation receive(Reservation reservation) {
    Reservation saved = reservations.save(reservation);
    events.publishEvent(new ReservationReceivedEvent(saved.getId()));
    return saved;
  }

  /**
   * 어떤 항목이 틀렸는지 드러나지 않도록 회원 예약, 연락처 불일치, 조회 기간 경과를 구분하지 않는다.
   */
  private boolean matchesGuest(Reservation reservation, PhoneNumber contactPhone) {
    return reservation.isGuest() && contactPhone.equals(reservation.getContactPhone())
        && withinGuestLookupPeriod(reservation);
  }

  private boolean withinGuestLookupPeriod(Reservation reservation) {
    LocalDateTime closedAt = reservation.getClosedAt();
    return closedAt == null
        || closedAt.plus(GUEST_LOOKUP_PERIOD).isAfter(LocalDateTime.now(clock));
  }

  /**
   * 이미 발급한 번호와 겹치지 않는 예약 번호. 동시에 같은 번호를 뽑는 드문 경우는 DB 유일 제약이 막는다.
   */
  private String newCode() {
    String code;
    do {
      code = ReservationCode.generate();
    } while (reservations.existsByCode(code));
    return code;
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
