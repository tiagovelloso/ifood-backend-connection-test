package br.com.ifood.chronos.restaurant.client;

import static org.springframework.http.HttpMethod.POST;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ifood.chronos.restaurant.client.exception.BusinessException;
import br.com.ifood.chronos.restaurant.client.exception.HttpClientError;
import br.com.ifood.chronos.restaurant.client.exception.ValidationException;
import br.com.ifood.chronos.restaurant.model.Restaurant;
import br.com.ifood.chronos.restaurant.model.Unavailability;
import br.com.ifood.chronos.restaurant.web.session.UserInformation;

@Component
public class ChronosSchedulerClientBean implements ChronosSchedulerClient {
	
	private final RestOperations rest = new RestTemplate();
	
	private final UserInformation userInfo;
	
	private final ObjectMapper objectMapper;
	
	@Value("${chronos.scheduler.service.address}")
	private String chronosSchedulerServiceAddress;
	
	
	public ChronosSchedulerClientBean(UserInformation userInfo, ObjectMapper objectMapper) {
		this.userInfo = userInfo;
		this.objectMapper = objectMapper;
	}

	@Override
	public Optional<Restaurant> getByLogin(String login) {
		Restaurant[] restaurants = rest.getForObject(chronosSchedulerServiceAddress + "/restaurants?login=" + login, Restaurant[].class);
		
		if (restaurants.length == 1) {
			return Optional.of(restaurants[0]);
		}
		
		return Optional.empty();
	}
	
	@Override
	public List<Unavailability> getUnavailabilities() {
		Unavailability[] unavailabilities = rest.getForObject(unavailabilityURI(), Unavailability[].class);
		return Arrays.asList(unavailabilities);
	}
	
	@Override
	public void createUnavailability(Unavailability unavailability) {
		try {
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			
			rest.exchange(unavailabilityURI(), POST, new HttpEntity<>(unavailability, httpHeaders), Void.class);
		} catch (HttpClientErrorException ex) {
			handleHttpClientErrorException(ex);
		}
	}
	
	@Override
	public void deleteUnavailability(Unavailability unavailability) {
		try {
			rest.delete(unavailabilityURI() + "/" + unavailability.getId());
		} catch (HttpClientErrorException ex) {
			handleHttpClientErrorException(ex);
		}
	}
	
	private String baseURI() {
		return chronosSchedulerServiceAddress + "/restaurants/" + userInfo.getRestaurant().getId();
	}
	
	private String unavailabilityURI() {
		return baseURI() + "/unavailabilities";
	}

	private void handleHttpClientErrorException(HttpClientErrorException ex) {
		switch (ex.getStatusCode()) {
		case UNPROCESSABLE_ENTITY:
			throw new BusinessException(HttpClientError.of(ex, objectMapper).getErrorsAsString());
		case BAD_REQUEST:
			throw new ValidationException(HttpClientError.of(ex, objectMapper).getErrorsAsString());
		default:
			throw ex;
		}
	}
}