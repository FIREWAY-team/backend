package com.fireway.backend.modules.incidents.domain;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * 신고 한 건.
 *
 * 좌표는 POINT 가 아니라 lat/lon 으로 둔다. scenarios 와 같은 방식이고,
 * 이 프로젝트가 축 순서로 여러 번 데인 만큼 공간 타입을 여기까지 늘리지 않는다.
 */
public record Incident(long id, String incidentNo, IncidentStatus status,
                       String address, double lat, double lon, String summary,
                       LocalDateTime receivedAt, LocalDateTime closedAt) {

    /** "2026-0915-" 까지의 길이. 채번 쿼리가 일련번호를 잘라낼 위치를 여기서 가져간다. */
    public static final int NUMBER_PREFIX_LENGTH = 10;

    /**
     * 접수번호 앞자리. 번호를 만드는 쪽과 그날 쓴 번호를 세는 쪽이 같은 규칙을 쓰도록
     * 한 군데에 둔다. 전체 번호는 여기에 네 자리 일련번호가 붙은 2026-0915-0001 형식이다.
     */
    public static String numberPrefix(LocalDate date) {
        return "%d-%02d%02d-".formatted(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    /** 접수 직후 상태. id 는 저장 시점에 채워진다. */
    public static Incident received(String incidentNo, String address, double lat, double lon,
                                    String summary, LocalDateTime receivedAt) {
        return new Incident(0, incidentNo, IncidentStatus.RECEIVED, address, lat, lon,
                summary == null ? "" : summary, receivedAt, null);
    }

    public Incident withId(long assigned) {
        return new Incident(assigned, incidentNo, status, address, lat, lon, summary, receivedAt, closedAt);
    }
}
