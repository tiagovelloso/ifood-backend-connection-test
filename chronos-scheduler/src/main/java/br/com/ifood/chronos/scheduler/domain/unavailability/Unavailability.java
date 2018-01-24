package br.com.ifood.chronos.scheduler.domain.unavailability;

import static javax.persistence.GenerationType.SEQUENCE;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import br.com.ifood.chronos.scheduler.domain.Restaurant;
import br.com.ifood.chronos.scheduler.service.RestaurantService.CreateUnavailabilityValidation;

@Entity
@SequenceGenerator(name="SQ_UNAVAILABILITY", sequenceName="SQ_UNAVAILABILITY", allocationSize=1)
public class Unavailability {
	
	@Null(groups=CreateUnavailabilityValidation.class)
	@Id
	@GeneratedValue(strategy=SEQUENCE, generator="SQ_UNAVAILABILITY")
	private Long id;
	
	@Null(groups=CreateUnavailabilityValidation.class)
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="restaurant", nullable=false)
	private Restaurant restaurant;
	
	@NotNull(groups=CreateUnavailabilityValidation.class)
	@Column(nullable=false)
	private Reason reason;
	
	@NotNull(groups=CreateUnavailabilityValidation.class)
	@Column(nullable=false)
	private LocalDateTime start;
	
	@NotNull(groups=CreateUnavailabilityValidation.class)
	@Column(nullable=false)
	private LocalDateTime end;
	
	@Null(groups=CreateUnavailabilityValidation.class)
	@Column(nullable=false)
	private LocalDateTime creation;
	
	
	public Unavailability() {
		
	}
	
	public Unavailability(Unavailability unavailability) {
		this.id = unavailability.id;
		this.restaurant = unavailability.restaurant;
		this.reason = unavailability.reason;
		this.start = unavailability.start;
		this.end = unavailability.end;
		this.creation = unavailability.creation;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Restaurant getRestaurant() {
		return restaurant;
	}

	public void setRestaurant(Restaurant restaurant) {
		this.restaurant = restaurant;
	}

	public Reason getReason() {
		return reason;
	}

	public void setReason(Reason reason) {
		this.reason = reason;
	}

	public LocalDateTime getStart() {
		return start;
	}

	public void setStart(LocalDateTime start) {
		this.start = start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	public void setEnd(LocalDateTime end) {
		this.end = end;
	}
	
	public LocalDateTime getCreation() {
		return creation;
	}

	public void setCreation(LocalDateTime creation) {
		this.creation = creation;
	}
}