package com.fireway.backend.modules.incidents.domain;
/** 신고 생명주기. 전이 규칙은 {@link #canMoveTo} 한 군데에 둔다. */
public enum IncidentStatus {
    RECEIVED,    // 접수
    DISPATCHED,  // 출동지령
    ON_SCENE,    // 현장도착
    CLOSED,      // 종결
    CANCELLED;   // 오인 · 취소

    /** 더 이상 전이가 없는 상태. */
    public boolean terminal() { return this == CLOSED || this == CANCELLED; }

    /**
     * 앞으로만 간다. 단계를 건너뛰는 것은 허용한다(접수 직후 오인 종결 등).
     * CANCELLED 가 맨 뒤라 진행 중이면 언제든 취소할 수 있고, 종결·취소 뒤로는 움직이지 않는다.
     */
    public boolean canMoveTo(IncidentStatus next) { return !terminal() && next.ordinal() > ordinal(); }
}
