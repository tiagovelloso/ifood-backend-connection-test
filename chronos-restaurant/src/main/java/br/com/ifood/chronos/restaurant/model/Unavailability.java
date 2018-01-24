package br.com.ifood.chronos.restaurant.model;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

public class Unavailability {
	
	private Long id;
	
	private Restaurant restaurant;
	
	private String reason;
	
	@DateTimeFormat(pattern="MM/dd/yyyy HH:mm")
	private LocalDateTime start;
	
	@DateTimeFormat(pattern="MM/dd/yyyy HH:mm")
	private LocalDateTime end;
	
	private LocalDateTime creation;
	

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

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
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