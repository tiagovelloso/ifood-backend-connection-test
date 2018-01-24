package br.com.ifood.chronos.scheduler.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.ifood.chronos.scheduler.domain.Restaurant;
import br.com.ifood.chronos.scheduler.domain.unavailability.Unavailability;

public interface UnavailabilityRepository extends JpaRepository<Unavailability, Long> {

	Optional<Unavailability> findById(Long id);
	
	Optional<Unavailability> findByIdAndRestaurant(Long id, Restaurant restaurant);

	List<Unavailability> findByRestaurantOrderByStartDesc(Restaurant restaurant);

	@Query("SELECT count(u) > 0 FROM #{#entityName} u WHERE u.restaurant = ?1 AND (u.start BETWEEN ?2 AND ?3 OR u.end BETWEEN ?2 AND ?3)")
	boolean existsByRestaurantAndPeriod(Restaurant restaurant, LocalDateTime start, LocalDateTime end);
}