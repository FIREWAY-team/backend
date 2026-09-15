package com.fireway.backend.modules.incidents.domain;
/** 신고 생명주기. 전이 규칙은 IncidentService 가 강제한다. */
public enum IncidentStatus {
    RECEIVED,    // 접수
    DISPATCHED,  // 출동지령
    ON_SCENE,    // 현장도착
    CLOSED,      // 종결
    CANCELLED;   // 오인 · 취소

    /** 더 이상 전이가 없는 상태. */
    public boolean terminal() { return this == CLOSED || this == CANCELLED; }
}
