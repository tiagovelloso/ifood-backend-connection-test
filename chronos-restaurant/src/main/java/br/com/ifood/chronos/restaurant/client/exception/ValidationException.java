package br.com.ifood.chronos.restaurant.client.exception;

import java.util.List;

public class ValidationException extends AbstractClientException {

	private static final long serialVersionUID = 1L;
	
	public ValidationException(List<String> messages) {
		super(messages);
	}
}
