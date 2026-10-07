package com.dacare.server.domain;

/**
 * 상담 한 건이 끝난 방식. CANCELLED는 서버 기한 전에 클라이언트가 연결을 끊은 경우다.
 */
public enum DiagnosisOutcome {COMPLETED, FAILED, TIMEOUT, CANCELLED}
