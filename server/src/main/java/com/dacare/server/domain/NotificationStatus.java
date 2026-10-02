package com.dacare.server.domain;

/**
 * 알림 발송 결과. SKIPPED는 설정 미완료나 주간 한도 초과로 발송을 시도하지 않은 경우다.
 */
public enum NotificationStatus {SUCCESS, FAILED, SKIPPED}
