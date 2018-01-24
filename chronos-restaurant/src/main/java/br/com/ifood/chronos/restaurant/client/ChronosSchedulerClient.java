package br.com.ifood.chronos.restaurant.client;

import java.util.List;
import java.util.Optional;

import br.com.ifood.chronos.restaurant.model.Restaurant;
import br.com.ifood.chronos.restaurant.model.Unavailability;

public interface ChronosSchedulerClient {

	Optional<Restaurant> getByLogin(String login);

	void createUnavailability(Unavailability unavailability);

	List<Unavailability> getUnavailabilities();

	void deleteUnavailability(Unavailability unavailability);

}