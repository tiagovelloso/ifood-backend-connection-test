package br.com.ifood.chronos.monitor.exception;

public class RestaurantNotFoundException extends ChronosMonitorException {

	private static final long serialVersionUID = 1L;
	
	private final Long id;

	public RestaurantNotFoundException(Long id) {
		this.id = id;
	}
	
	public Long getId() {
		return id;
	}
}