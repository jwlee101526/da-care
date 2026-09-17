package com.dacare.server.service;
import com.dacare.server.domain.*;
import com.dacare.server.notification.NotificationSender;
import com.dacare.server.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    private final AppUserRepository users; private final CustomerRepository customers; private final EngineerRepository engineers; private final ReservationRepository reservations; private final ApplicationEventPublisher events;
    public ReservationService(AppUserRepository users, CustomerRepository customers, EngineerRepository engineers, ReservationRepository reservations, ApplicationEventPublisher events) { this.users = users; this.customers = customers; this.engineers = engineers; this.reservations = reservations; this.events = events; }
    @Transactional public Reservation create(String email, String deviceType, String symptom, String address, LocalDateTime preferredAt) { if (!preferredAt.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("희망 방문 일시는 미래여야 합니다."); AppUser user = users.findByEmail(email).orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다.")); Customer customer = customers.findByUser(user).orElseThrow(() -> new NoSuchElementException("고객 정보를 찾을 수 없습니다.")); return reservations.save(new Reservation(customer, deviceType, symptom, address, preferredAt)); }
    public List<Reservation> mine(String email) { AppUser user = users.findByEmail(email).orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다.")); return reservations.findAllByCustomerOrderByCreatedAtDesc(customers.findByUser(user).orElseThrow(() -> new NoSuchElementException("고객 정보를 찾을 수 없습니다."))); }
    public List<Reservation> all() { return reservations.findAll(); }
    @Transactional public Reservation confirm(Long reservationId, Long engineerId, LocalDateTime confirmedAt) { if (!confirmedAt.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("확정 방문 일시는 미래여야 합니다."); Reservation reservation = reservations.findById(reservationId).orElseThrow(() -> new NoSuchElementException("예약을 찾을 수 없습니다.")); Engineer engineer = engineers.findById(engineerId).orElseThrow(() -> new NoSuchElementException("기사를 찾을 수 없습니다.")); reservation.confirm(engineer, confirmedAt); events.publishEvent(new ReservationConfirmedEvent(reservation.getId())); return reservation; }
}
record ReservationConfirmedEvent(Long reservationId) {}
