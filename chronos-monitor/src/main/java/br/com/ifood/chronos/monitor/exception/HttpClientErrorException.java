package br.com.ifood.chronos.monitor.exception;

import akka.http.javadsl.model.StatusCode;

public final class HttpClientErrorException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private final StatusCode status;
	
	private final String responseBody;
	
	public HttpClientErrorException(StatusCode status) {
		this.status = status;
		this.responseBody = null;
	}
	
	public HttpClientErrorException(StatusCode status, String responseBody) {
		this.status = status;
		this.responseBody = responseBody;
	}
	
	public StatusCode getStatus() {
		return status;
	}
	
	public String getResponseBody() {
		return responseBody;
	}
	
	@Override
	public String getMessage() {
		return String.format("HTTP Status %d with content %s", status.intValue(), responseBody);
	}
	
	public Boolean is1xx() {
		return status.intValue() / 100 == 1;
	}
	
	public Boolean is2xx() {
		return status.intValue() / 100 == 2;
	}
	
	public Boolean is3xx() {
		return status.intValue() / 100 == 3;
	}
	
	public Boolean is4xx() {
		return status.intValue() / 100 == 4;
	}
	
	public Boolean is5xx() {
		return status.intValue() / 100 == 5;
	}
	
	public static Boolean is4xx(Throwable t) {
		return t instanceof HttpClientErrorException && ((HttpClientErrorException) t).is4xx();
	}
	
	public static Boolean is(Throwable t, StatusCode statusCode) {
		return t != null && t instanceof HttpClientErrorException && statusCode.equals(((HttpClientErrorException) t).getStatus());
	}
	
	public static String getResponseBody(Throwable t) {
		return t instanceof HttpClientErrorException ? ((HttpClientErrorException)t).getResponseBody() : null;
	}
}