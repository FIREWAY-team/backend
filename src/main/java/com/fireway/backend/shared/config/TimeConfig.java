package com.fireway.backend.shared.config;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration public class TimeConfig {
    /**
     * 접수 시각과 접수번호가 이 시계를 따른다. 빈으로 빼두면 테스트가 시간을 고정할 수 있다.
     * application.yml 의 jackson.time-zone 과 같은 Asia/Seoul 을 쓴다.
     */
    @Bean Clock systemClock() { return Clock.system(ZoneId.of("Asia/Seoul")); }
}
