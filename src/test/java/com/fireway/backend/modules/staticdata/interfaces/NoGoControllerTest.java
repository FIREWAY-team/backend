package com.fireway.backend.modules.staticdata.interfaces;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fireway.backend.modules.staticdata.application.NoGoAreaService;
import com.fireway.backend.modules.staticdata.domain.Coordinate;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import com.fireway.backend.modules.staticdata.domain.NoGoArea.GeometryType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NoGoControllerTest {
    private static final NoGoArea LINE = new NoGoArea(3, "eunhaeng1-impassable-001", "은행1동",
            "소방차 진입곤란 지정", 1, NoGoArea.OK, "", GeometryType.LINE_STRING,
            List.of(new Coordinate(37.438, 127.142), new Coordinate(37.439, 127.143)));

    // 위성 대조에서 도로가 아닌 것으로 확인된 구간. 지도에는 내려가고 라우팅에서는 빠진다.
    private static final NoGoArea GHOST = new NoGoArea(9, "geumgwang1-impassable-003", "금광1동",
            "소방차 진입곤란 지정", 1, NoGoArea.UNVERIFIED, "지도상 미확인", GeometryType.LINE_STRING,
            List.of(new Coordinate(37.441, 127.151), new Coordinate(37.442, 127.152)));

    // standaloneSetup 은 부트 자동설정을 안 태우므로 snake_case 를 직접 걸어준다(JacksonConfig 와 동일).
    private MockMvc mvc(NoGoArea... areas) {
        var converter = new MappingJackson2HttpMessageConverter(
                new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE));
        return MockMvcBuilders.standaloneSetup(new NoGoController(new NoGoAreaService(() -> List.of(areas))))
                .setMessageConverters(converter)
                .setControllerAdvice(new com.fireway.backend.shared.exception.GlobalExceptionHandler())
                .build();
    }

    @Test void 좌표를_lon_lat_순서로_내려준다() throws Exception {
        mvc(LINE).perform(get("/api/no_go"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path[0][0]").value(127.142))
                .andExpect(jsonPath("$[0].path[0][1]").value(37.438));
    }

    @Test void 출처와_도형_종류를_함께_내려준다() throws Exception {
        mvc(LINE).perform(get("/api/no_go"))
                .andExpect(jsonPath("$[0].ext_id").value("eunhaeng1-impassable-001"))
                .andExpect(jsonPath("$[0].geometry_type").value("LineString"))
                .andExpect(jsonPath("$[0].dong").value("은행1동"))
                .andExpect(jsonPath("$[0].note").value(""));
    }

    @Test void 검증_상태를_내려준다() throws Exception {
        mvc(LINE, GHOST).perform(get("/api/no_go"))
                .andExpect(jsonPath("$[0].verification_status").value("ok"))
                .andExpect(jsonPath("$[1].verification_status").value("unverified"))
                .andExpect(jsonPath("$[1].note").value("지도상 미확인"));
    }

    // 지도 표시용 목록에서는 빼지 않는다. 상황실이 재검증해야 하기 때문이다.
    @Test void 미확인_구간도_목록에는_내려간다() throws Exception {
        mvc(LINE, GHOST).perform(get("/api/no_go")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test void 데이터가_없어도_200_이다() throws Exception {
        mvc().perform(get("/api/no_go")).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    // 전건이 1,276건이라 지도를 움직일 때마다 다 내려주면 안 된다. 화면 범위만 받는다.
    @Test void bbox_밖의_구간은_빠진다() throws Exception {
        mvc(LINE, GHOST).perform(get("/api/no_go").param("bbox", "127.140,37.435,127.145,37.440"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ext_id").value("eunhaeng1-impassable-001"));
    }

    /**
     * bbox 는 GeoJSON 과 같은 lon 먼저 순서다. 파싱이 lat 먼저로 뒤집히면 이 범위가
     * 아무것도 못 잡아 0건이 된다 — 예외 없이 조용히 비므로 테스트로 못을 박는다.
     */
    @Test void bbox_는_lon_이_먼저다() throws Exception {
        mvc(LINE).perform(get("/api/no_go").param("bbox", "127.140,37.435,127.145,37.440"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test void bbox_가_망가지면_422_다() throws Exception {
        mvc(LINE).perform(get("/api/no_go").param("bbox", "127.140,37.435,127.145"))
                .andExpect(status().isUnprocessableEntity());
        mvc(LINE).perform(get("/api/no_go").param("bbox", "동쪽,37.435,127.145,37.440"))
                .andExpect(status().isUnprocessableEntity());
        // 최소값이 최대값보다 큰 경우. BoundingBox 생성자가 막는데 500 으로 새면 안 된다.
        mvc(LINE).perform(get("/api/no_go").param("bbox", "127.145,37.440,127.140,37.435"))
                .andExpect(status().isUnprocessableEntity());
    }

    // 재임포트 전에는 안 바뀌는 정적 데이터다. 지도를 움직일 때마다 같은 응답을 다시 받지 않게 한다.
    @Test void 캐시_헤더를_붙인다() throws Exception {
        mvc(LINE).perform(get("/api/no_go"))
                .andExpect(header().string("Cache-Control", "max-age=300, public"));
        mvc(LINE).perform(get("/api/no_go").param("bbox", "127.140,37.435,127.145,37.440"))
                .andExpect(header().string("Cache-Control", "max-age=300, public"));
    }
}
