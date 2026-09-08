package com.fireway.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 뼈대 단계의 최소 테스트.
 *
 * 일부러 @SpringBootTest 를 쓰지 않았다. 그걸 붙이면 스프링 컨텍스트가 뜨면서
 * DataSource 연결을 시도하는데, DB가 없는 환경(CI, 다른 팀원 로컬)에서는
 * 코드 잘못이 없어도 빌드가 깨진다.
 *
 * DB가 정해진 뒤에 @SpringBootTest 나 @DataJpaTest 를 추가할 것.
 */
class BackendApplicationTests {

    @Test
    void 테스트_실행_환경이_정상이다() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
