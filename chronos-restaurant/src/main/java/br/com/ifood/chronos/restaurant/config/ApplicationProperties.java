package br.com.ifood.chronos.restaurant.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("appProps")
public class ApplicationProperties {
	
	@Value("${chronos.monitor.service.address}")
	private String chronosMonitorServiceAddress;
	
	@Value("#{${chronos.unavailability.reasons}}")
	private Map<String, String> reasons;
	
	public String getChronosMonitorServiceAddress() {
		return chronosMonitorServiceAddress;
	}

	public void setChronosMonitorServiceAddress(String chronosMonitorServiceAddress) {
		this.chronosMonitorServiceAddress = chronosMonitorServiceAddress;
	}

	public void setReasons(Map<String, String> reasons) {
		this.reasons = reasons;
	}
	
	public Map<String, String> getReasons() {
		return reasons;
	}	
}