package com.fireway.backend.modules.vehicles.interfaces;
import java.util.List;
import com.fireway.backend.modules.vehicles.application.VehicleService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/vehicles") public class VehicleController { private final VehicleService service; public VehicleController(VehicleService service) { this.service = service; } @GetMapping public List<VehicleResponse> list() { return service.mock().stream().map(v -> new VehicleResponse(v.vehicleId(), v.name(), v.widthM(), v.heightM(), v.lengthM(), v.weightTon(), v.turningRadiusM())).toList(); } public record VehicleResponse(String vehicleId, String name, double widthM, double heightM, double lengthM, double weightTon, double turningRadiusM) { } }
