package br.com.ifood.chronos.monitor.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class OfflineLog {
	
	private final Restaurant restaurant;
	
	private final LocalDateTime start;
	
	private final LocalDateTime end;
	
	private final Long offlineTime;

	public OfflineLog(
			@JsonProperty("restaurant") Restaurant restaurant, 
			@JsonProperty("start") LocalDateTime start, 
			@JsonProperty("end") LocalDateTime end,
			@JsonProperty("offlineTime") Long offlineTime) {
		this.restaurant = restaurant;
		this.start = start;
		this.end = end;
		this.offlineTime = offlineTime;
	}

	public Restaurant getRestaurant() {
		return restaurant;
	}

	public LocalDateTime getStart() {
		return start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	public Long getOfflineTime() {
		return offlineTime;
	}
}