package br.com.ifood.chronos.restaurant.web.session;

import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import br.com.ifood.chronos.restaurant.model.Restaurant;

@Component("userInfo")
@SessionScope(proxyMode=TARGET_CLASS)
public class UserInformation {
	
	private Restaurant restaurant;

	public Restaurant getRestaurant() {
		return restaurant;
	}

	public void setRestaurant(Restaurant restaurant) {
		this.restaurant = restaurant;
	}
	
	public void clear() {
		this.restaurant = null;
	}
}