package com.dacare.server.notification;
import com.dacare.server.domain.Reservation;
public interface NotificationSender { String channel(); void send(Reservation reservation); }
