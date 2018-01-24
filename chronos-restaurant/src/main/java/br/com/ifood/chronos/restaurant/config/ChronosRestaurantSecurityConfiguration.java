package br.com.ifood.chronos.restaurant.config;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import br.com.ifood.chronos.restaurant.client.ChronosSchedulerClient;
import br.com.ifood.chronos.restaurant.model.Restaurant;
import br.com.ifood.chronos.restaurant.web.session.UserInformation;

@Configuration
@Component
public class ChronosRestaurantSecurityConfiguration extends WebSecurityConfigurerAdapter {
	
	private final ChronosSchedulerClient chronosScheduler;
	
	private final UserInformation userInformation;
	
	public ChronosRestaurantSecurityConfiguration(ChronosSchedulerClient chronosScheduler, UserInformation userInformation) {
		this.chronosScheduler = chronosScheduler;
		this.userInformation = userInformation;
	}

	@Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    	auth.authenticationProvider(new AuthenticationProvider() {
			@Override
			public boolean supports(Class<?> authentication) {
				return authentication.equals(UsernamePasswordAuthenticationToken.class);
			}
			
			@Override
			public Authentication authenticate(Authentication authentication) throws AuthenticationException {
				userInformation.clear();
				
				String name = authentication.getName();
				String password = authentication.getCredentials().toString();
				
				Optional<Restaurant> restaurant = chronosScheduler.getByLogin(name);
				
				if (restaurant.isPresent() && password == null || password.equals("")) {
					userInformation.setRestaurant(restaurant.get());
					return new UsernamePasswordAuthenticationToken(restaurant.get(), password, new ArrayList<>());
				}
				
				throw new UsernameNotFoundException(name);
			}
		});
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.httpBasic().and().authorizeRequests().anyRequest().authenticated();
	}
}