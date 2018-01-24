package br.com.ifood.chronos.scheduler.domain;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.ifood.chronos.scheduler.domain.unavailability.Unavailability;
import br.com.ifood.chronos.scheduler.service.RestaurantService.CreateOfflineLogValidation;
import br.com.ifood.chronos.scheduler.service.RestaurantService.CreateRestaurantValidation;

@Entity
@SequenceGenerator(name="SQ_RESTAURANT", sequenceName="SQ_RESTAURANT", allocationSize=1)
public class Restaurant {
	
	@Null(groups=CreateRestaurantValidation.class)
	@NotNull(groups=CreateOfflineLogValidation.class)
	@Id
	@GeneratedValue(strategy=SEQUENCE, generator="SQ_RESTAURANT")
	private Long id;
	
	@NotNull(groups=CreateRestaurantValidation.class)
	@Column(nullable=false, unique=true)
	private String login;
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="restaurant")
	private Set<Unavailability> unavailabilities;
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="restaurant")
	private Set<OfflineLog> offlineLogs;
	
	@JsonProperty
	@Transient
	private Long offlineTime;

	
	public Restaurant() {
		
	}
	
	public Restaurant(Long id) {
		this.id = id;
	}
	
	public Restaurant(Restaurant restaurant) {
		this.id = restaurant.id;
		this.login = restaurant.login;
		this.unavailabilities = restaurant.unavailabilities;
	}
	
	public Restaurant(Restaurant restaurant, Long offlineTime) {
		this(restaurant);
		this.offlineTime = offlineTime;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public Set<Unavailability> getUnavailabilities() {
		return unavailabilities;
	}

	public void setUnavailabilities(Set<Unavailability> unavailabilities) {
		this.unavailabilities = unavailabilities;
	}
	
	public Set<OfflineLog> getOfflineLogs() {
		return offlineLogs;
	}

	public void setOfflineLogs(Set<OfflineLog> offlineLogs) {
		this.offlineLogs = offlineLogs;
	}

	public Long getOfflineTime() {
		return offlineTime;
	}

	public void setOfflineTime(Long offlineTime) {
		this.offlineTime = offlineTime;
	}
}