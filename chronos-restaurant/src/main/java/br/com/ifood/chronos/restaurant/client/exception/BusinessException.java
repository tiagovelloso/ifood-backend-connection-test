package br.com.ifood.chronos.restaurant.client.exception;

import java.util.List;

public class BusinessException extends AbstractClientException {
	
	private static final long serialVersionUID = 1L;
	
	public BusinessException(List<String> messages) {
		super(messages);
	}
}