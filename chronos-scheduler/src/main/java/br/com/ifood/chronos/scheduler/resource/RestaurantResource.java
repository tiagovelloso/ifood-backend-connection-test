package br.com.ifood.chronos.scheduler.resource;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.ifood.chronos.scheduler.domain.OfflineLog;
import br.com.ifood.chronos.scheduler.domain.Restaurant;
import br.com.ifood.chronos.scheduler.domain.unavailability.Unavailability;
import br.com.ifood.chronos.scheduler.service.RestaurantService;
import br.com.ifood.chronos.scheduler.service.RestaurantService.CreateOfflineLogValidation;
import br.com.ifood.chronos.scheduler.service.RestaurantService.CreateRestaurantValidation;
import br.com.ifood.chronos.scheduler.service.RestaurantService.CreateUnavailabilityValidation;

@RestController
@RequestMapping("/restaurants")
public class RestaurantResource {
	
	private final RestaurantService restaurantService;
	
	public RestaurantResource(RestaurantService restaurantService) {
		this.restaurantService = restaurantService;
	}
	
	@RequestMapping(method=GET)
	ResponseEntity<List<Restaurant>> get(@RequestParam(name="login") String login) {
		List<Restaurant> restaurants = new ArrayList<Restaurant>();
		restaurantService.getRestaurant(login).ifPresent(restaurants::add);
		return new ResponseEntity<>(restaurants, OK);
	}
	
	@RequestMapping(value="/{id}", method=GET)
	ResponseEntity<Restaurant> get(@PathVariable("id") Long id) {
		Optional<Restaurant> restaurant = restaurantService.getRestaurant(id);
		return restaurant.isPresent() ? new ResponseEntity<>(restaurant.get(), OK) : new ResponseEntity<>(NOT_FOUND);
	}
	
	@RequestMapping(method=POST)
	ResponseEntity<Void> post(@RequestBody @Validated(CreateRestaurantValidation.class) Restaurant restaurant) {
		Restaurant createdRestaurant = restaurantService.createRestaurant(restaurant);
		return ResponseEntity.created(fromCurrentRequest().path("/{id}").buildAndExpand(createdRestaurant.getId()).toUri()).build();
	}
	
	@RequestMapping(value="/{id}/unavailabilities", method=GET)
	ResponseEntity<List<Unavailability>> getUnavailabilities(@PathVariable("id") Long id) {
		return new ResponseEntity<>(restaurantService.getUnavailabilities(new Restaurant(id)), OK);
	}
	
	@RequestMapping(value="/{id}/unavailabilities/{unavailabilityId}", method=GET)
	ResponseEntity<Unavailability> getUnavailability(@PathVariable("id") Long id, @PathVariable("unavailabilityId") Long unavailabilityId) {
		Optional<Unavailability> unavailability = restaurantService.getUnavailability(unavailabilityId, new Restaurant(id));
		return unavailability.isPresent() ? new ResponseEntity<>(unavailability.get(), OK) : new ResponseEntity<>(NOT_FOUND);
	}
	
	@RequestMapping(value="/{id}/unavailabilities/{unavailabilityId}", method=DELETE)
	ResponseEntity<Void> delete(@PathVariable("id") Long id, @PathVariable("unavailabilityId") Long unavailabilityId) {
		restaurantService.deleteUnavailability(unavailabilityId, new Restaurant(id));
		return new ResponseEntity<>(OK);
	}

	@RequestMapping(value="/{id}/unavailabilities", method=POST)
	ResponseEntity<Void> postUnavailability(@PathVariable("id") Long id,
			@RequestBody @Validated(CreateUnavailabilityValidation.class) Unavailability unavailability) {
		unavailability.setRestaurant(new Restaurant(id));
		Unavailability createdUnavailability = restaurantService.createUnavailability(unavailability);
		return ResponseEntity.created(fromCurrentRequest().path("/{id}").buildAndExpand(createdUnavailability.getId()).toUri()).build();
	}
	
	
	@RequestMapping(value="/{id}/offlinelogs", method=POST)
	ResponseEntity<Void> postOfflineLog(@PathVariable("id") Long id,
			@RequestBody @Validated(CreateOfflineLogValidation.class) OfflineLog log) {
		log.setRestaurant(new Restaurant(id));
		OfflineLog createdOfflineLog = restaurantService.createOfflineLog(log);
		return ResponseEntity.created(fromCurrentRequest().path("/{id}").buildAndExpand(createdOfflineLog.getId()).toUri()).build();
	}
	
	@RequestMapping(value="/{id}/offlinelogs", method=GET)
	ResponseEntity<List<OfflineLog>> getOfflineLogs(@PathVariable("id") Long id) {
		return new ResponseEntity<>(restaurantService.getOfflineLogs(new Restaurant(id)), OK);
	}
	
	@RequestMapping(value = "/ranking", method = GET)
	ResponseEntity<List<Restaurant>> getRanking(
			@RequestParam(name="start") @DateTimeFormat(iso=DATE) LocalDate start,
			@RequestParam(name="end") @DateTimeFormat(iso=DATE) LocalDate end) {
		return new ResponseEntity<>(restaurantService.rank(start, end), OK);
	}
}