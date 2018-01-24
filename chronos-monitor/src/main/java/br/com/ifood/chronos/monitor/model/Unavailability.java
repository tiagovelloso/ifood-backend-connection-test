package br.com.ifood.chronos.monitor.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public final class Unavailability {
	
	private final LocalDateTime start;
	
	private final LocalDateTime end;
	
	public Unavailability(
			@JsonProperty("start") LocalDateTime start,
			@JsonProperty("end") LocalDateTime end) {
		this.start = (start != null) ? start.withSecond(0).withNano(0) : null;
		this.end = (end != null) ? end.withSecond(0).withNano(0) : null;
	}

	public LocalDateTime getStart() {
		return start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	@Override
	public String toString() {
		return "Unavailability [start=" + start + ", end=" + end + "]";
	}
}