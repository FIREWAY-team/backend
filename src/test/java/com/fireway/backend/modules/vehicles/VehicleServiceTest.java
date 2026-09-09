package com.fireway.backend.modules.vehicles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import com.fireway.backend.modules.vehicles.application.VehicleService;
import com.fireway.backend.modules.vehicles.application.port.VehicleRepository;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VehicleServiceTest {
    private final VehicleRepository repository = mock(VehicleRepository.class);
    private final VehicleService service = new VehicleService(repository);

    @Test
    void prefers_database_vehicle_over_fixture() {
        var vehicle = new Vehicle("pump-3.5", "DB 차량", 2.2, 3, 7, 4, 9);
        when(repository.findById("pump-3.5")).thenReturn(Optional.of(vehicle));
        assertThat(service.findById("pump-3.5")).contains(vehicle);
    }

    @Test
    void falls_back_only_to_known_mock_vehicles_when_database_has_no_row() {
        assertThat(service.findById("pump-3.5")).contains(service.mock().get(0));
        assertThat(service.findById("missing")).isEmpty();
        verify(repository).findById("pump-3.5");
        verify(repository).findById("missing");
    }
}
