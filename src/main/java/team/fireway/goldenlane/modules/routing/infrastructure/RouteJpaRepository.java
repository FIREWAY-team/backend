package team.fireway.goldenlane.modules.routing.infrastructure;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.stereotype.Repository;
@Repository public interface RouteJpaRepository extends JpaRepository<RouteProjection,String> {}

