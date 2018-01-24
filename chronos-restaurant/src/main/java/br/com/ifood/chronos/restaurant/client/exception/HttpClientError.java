package br.com.ifood.chronos.restaurant.client.exception;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpClientError {
	
	@JsonProperty private List<HttpClientErrorMessage> errors;
	
	public List<String> getErrorsAsString() {
		return errors.stream().map(e -> e.message).collect(Collectors.toList());
	}
	
	public static HttpClientError of(HttpClientErrorException ex, ObjectMapper mapper) {
		try {
			return mapper.readValue(ex.getResponseBodyAsString(), HttpClientError.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static class HttpClientErrorMessage {
		@JsonProperty private String message;
	}
}