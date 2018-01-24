package br.com.ifood.chronos.restaurant.client.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractClientException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private final List<String> messages;

	public AbstractClientException(List<String> messages) {
		this.messages = Collections.unmodifiableList(messages != null ? messages : new ArrayList<>());
	}
	
	public List<String> getMessages() {
		return messages;
	}
	
	@Override
	public String getMessage() {
		return messages.stream().collect(Collectors.joining(","));
	}
}