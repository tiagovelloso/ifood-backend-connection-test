package br.com.ifood.chronos.scheduler.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.ifood.chronos.scheduler.domain.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

	Optional<Restaurant> findById(Long id);

	Optional<Restaurant> findByLogin(String login);
	
	@Query("SELECT new br.com.ifood.chronos.scheduler.domain.Restaurant(r, SUM(l.offlineTime)) FROM Restaurant r JOIN r.offlineLogs l WHERE l.start BETWEEN ?1 AND ?2 GROUP BY r ORDER BY SUM(l.offlineTime)")
	List<Restaurant> rank(LocalDateTime start, LocalDateTime end);
	
}