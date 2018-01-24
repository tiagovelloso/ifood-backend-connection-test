package br.com.ifood.chronos.monitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Restaurant {
	
	private final Long id;

	public Restaurant(@JsonProperty("id") Long id) {
		this.id = id;
	}
	
	public Long getId() {
		return id;
	}

	@Override
	public String toString() {
		return "Restaurant [id=" + id + "]";
	}
}