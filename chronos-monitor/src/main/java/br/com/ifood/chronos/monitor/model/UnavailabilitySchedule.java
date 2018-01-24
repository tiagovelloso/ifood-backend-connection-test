package br.com.ifood.chronos.monitor.model;

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

public final class UnavailabilitySchedule {
	
	private final List<Unavailability> unavailabilities;

	public UnavailabilitySchedule(List<Unavailability> unavailabilities) {
		this.unavailabilities = unmodifiableList(unavailabilities == null ? new ArrayList<>() : unavailabilities);
	}
	
	public UnavailabilitySchedule(Unavailability[] unavailabilities) {
		this.unavailabilities = unmodifiableList(stream(unavailabilities).collect(toList()));
	}
	
	public List<Unavailability> getUnavailabilities() {
		return unavailabilities;
	}
	
}