package com.fireway.backend.modules.routing.infrastructure;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RouteJpaRepository extends JpaRepository<ScenarioEntity, String> { }
