package br.com.ifood.chronos.scheduler.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.ifood.chronos.scheduler.ChronosSchedulerApplicationTest;
import br.com.ifood.chronos.scheduler.domain.OfflineLog;
import br.com.ifood.chronos.scheduler.domain.Restaurant;
import br.com.ifood.chronos.scheduler.domain.unavailability.Reason;
import br.com.ifood.chronos.scheduler.domain.unavailability.Unavailability;
import br.com.ifood.chronos.scheduler.repository.OfflineLogRepository;
import br.com.ifood.chronos.scheduler.repository.RestaurantRepository;
import br.com.ifood.chronos.scheduler.repository.UnavailabilityRepository;
import br.com.ifood.chronos.scheduler.service.exception.business.ScheduleConflictException;
import br.com.ifood.chronos.scheduler.service.exception.business.ScheduleInvalidPeriodException;
import br.com.ifood.chronos.scheduler.service.exception.business.ScheduleTimePastException;

public class RestaurantServiceBeanTest extends ChronosSchedulerApplicationTest {
	
	@Autowired
	private RestaurantService restaurantService;
	
	@Autowired
	private RestaurantRepository restaurantRepository;
	
	@Autowired
	private UnavailabilityRepository unavailabilityRepository; 
	
	@Autowired
	private OfflineLogRepository offlineLogRepository;
	
	
	@After
	public void after() {
		unavailabilityRepository.deleteAll();
		offlineLogRepository.deleteAll();
		restaurantRepository.deleteAll();
	}
	
	@Test
	public void rankingRestaurantTest() {
		// Creating two restaurants
		Restaurant r1 = createRestaurant("R1");
		Restaurant r2 = createRestaurant("R2");
		
		// Creating offline log for both restaurants
		LocalDateTime start1 = LocalDateTime.now().minusDays(2);
		createOfflineLog(r1, start1, start1.plusHours(1), 300000L);
		createOfflineLog(r2, start1, start1.plusHours(1), 600000L);
		
		// Creating offline log only for restaurant 1
		LocalDateTime start2 = start1.plusDays(1);
		createOfflineLog(r1, start2, start2.plusHours(1), 600000L);
		
		// Ranking the restaurnts
		List<Restaurant> rank = restaurantService.rank(start1.toLocalDate(), LocalDateTime.now().toLocalDate());
		
		// Should have two restaurnts in ranking
		assertThat(rank.size()).isEqualTo(2);
		
		// The second restaurant should be at top of ranking with 10min offline
		assertThat(rank.get(0).getLogin()).isEqualTo(r2.getLogin());
		assertThat(rank.get(0).getOfflineTime()).isEqualTo(600000L);
		
		// The first restaurant should be at second of ranking with 15min offline
		assertThat(rank.get(1).getLogin()).isEqualTo(r1.getLogin());
		assertThat(rank.get(1).getOfflineTime()).isEqualTo(900000L);
	}
	
	@Test(expected=ScheduleTimePastException.class)
	public void createPastUnavailabilityTest() {
		Restaurant restaurant = createRestaurant("R1");
		createUnavailability(restaurant, LocalDateTime.now().minusMinutes(1), LocalDateTime.now());
	}
	
	@Test(expected=ScheduleInvalidPeriodException.class)
	public void createUnavailabilityInvalidIntervalTest() {
		Restaurant restaurant = createRestaurant("R1");
		createUnavailability(restaurant, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1));
	}
	
	@Test(expected=ScheduleConflictException.class)
	public void createUnavailabilityConflictTest() {
		Restaurant restaurant = createRestaurant("R1");
		
		LocalDateTime start = LocalDateTime.now().plusDays(2);
		LocalDateTime end = start.plusDays(1);
		
		Unavailability unavailability = createUnavailability(restaurant, start, end);
		
		assertThat(unavailability.getId()).isNotNull();
		
		createUnavailability(restaurant, start.minusHours(5), end.plusDays(1));
	}
	
	private Restaurant createRestaurant(String login) {
		Restaurant restaurant = new Restaurant();
		restaurant.setLogin(login);
		return restaurantService.createRestaurant(restaurant);
	}
	
	private Unavailability createUnavailability(Restaurant restaurant, LocalDateTime start, LocalDateTime end) {
		Unavailability unavailability = new Unavailability();
		
		unavailability.setRestaurant(restaurant);
		unavailability.setReason(Reason.HOLIDAY);
		unavailability.setStart(start);
		unavailability.setEnd(end);
		
		return restaurantService.createUnavailability(unavailability);
	}
	
	private OfflineLog createOfflineLog(Restaurant restaurant, LocalDateTime start, LocalDateTime end, Long offlineTime) {
		OfflineLog log = new OfflineLog();
		
		log.setRestaurant(restaurant);
		log.setStart(start);
		log.setEnd(end);
		log.setOfflineTime(offlineTime);
		
		return restaurantService.createOfflineLog(log);
	}
}