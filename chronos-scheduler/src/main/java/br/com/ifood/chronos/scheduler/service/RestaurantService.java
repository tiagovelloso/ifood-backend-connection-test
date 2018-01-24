package br.com.ifood.chronos.scheduler.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import br.com.ifood.chronos.scheduler.domain.OfflineLog;
import br.com.ifood.chronos.scheduler.domain.Restaurant;
import br.com.ifood.chronos.scheduler.domain.unavailability.Unavailability;

public interface RestaurantService {
	
	Restaurant createRestaurant(Restaurant restaurant);
	
	Optional<Restaurant> getRestaurant(Long id);
	
	Optional<Restaurant> getRestaurant(String login);
	
	
	Unavailability createUnavailability(Unavailability unavailability);

	Optional<Unavailability> getUnavailability(Long id, Restaurant restaurant);

	List<Unavailability> getUnavailabilities(Restaurant restaurant);

	void deleteUnavailability(Long id, Restaurant restaurant);

	
	OfflineLog createOfflineLog(OfflineLog log);

	List<OfflineLog> getOfflineLogs(Restaurant restaurant);

	List<Restaurant> rank(LocalDate start, LocalDate end);
	
	
	/** Message class to notify listeners about changes in restaurants */
	public final class RestaurantChangeEvent {
		private final Restaurant restaurant;

		public RestaurantChangeEvent(Restaurant restaurant) {
			super();
			this.restaurant = restaurant;
		}
		
		public Restaurant getRestaurant() {
			return restaurant;
		}
	}
	
	// Validation Interfaces
	
	public interface CreateRestaurantValidation { }
	
	public interface CreateUnavailabilityValidation { }
	
	public interface CreateOfflineLogValidation { }
	
	
	
}