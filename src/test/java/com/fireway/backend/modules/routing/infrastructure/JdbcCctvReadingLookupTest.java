package com.fireway.backend.modules.routing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JdbcCctvReadingLookupTest {

    private final JdbcCctvReadingLookup lookup =
            new JdbcCctvReadingLookup(null, new ObjectMapper());

    @Test
    void parsesPerVehicleVerdictsAndRejectsMalformedJson() {
        assertThat(lookup.parseVerdict("{\"pump-3.5\":\"PASS\",\"pump-8\":\"FAIL\"}"))
                .containsEntry("pump-3.5", "PASS")
                .containsEntry("pump-8", "FAIL");
        assertThat(lookup.parseVerdict("not-json")).isEmpty();
    }
}
