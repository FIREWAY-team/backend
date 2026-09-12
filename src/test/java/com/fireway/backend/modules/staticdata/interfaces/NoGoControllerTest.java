package com.fireway.backend.modules.staticdata.interfaces;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .setMessageConverters(converter).build();
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
}
