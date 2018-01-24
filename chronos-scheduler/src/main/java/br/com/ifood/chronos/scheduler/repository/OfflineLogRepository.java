package br.com.ifood.chronos.scheduler.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.ifood.chronos.scheduler.domain.OfflineLog;
import br.com.ifood.chronos.scheduler.domain.Restaurant;

public interface OfflineLogRepository extends JpaRepository<OfflineLog, Long> {

	List<OfflineLog> findByRestaurant(Restaurant restaurant);

}