package br.com.ifood.chronos.scheduler.service.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

import br.com.ifood.chronos.scheduler.service.RestaurantService.RestaurantChangeEvent;

/**
 * When any change occurs to restaurant, this listener is
 * responsible to send a notification to monitor module.
 * 
 * @author Tiago Velloso <tiago.velloso@gmail.com>
 */
@Component
@Profile("!test")
public class RestaurantChangeEventListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(RestaurantChangeEventListener.class);
	
	@Value("${chronos.monitor.service.address}")
	private String chronosMonitorServiceAddress;
	
	private final RestTemplate restTemplate = new RestTemplate();
	
	@Async
	@TransactionalEventListener
	void restaurantChangeEventListener(RestaurantChangeEvent event) {
		LOG.info("Notifying monitor module about changes in restaurant {}", event.getRestaurant().getId());
		restTemplate.put(String.format("%s/restaurants/%d/unavailabilities", chronosMonitorServiceAddress, event.getRestaurant().getId()), null);
	}
}