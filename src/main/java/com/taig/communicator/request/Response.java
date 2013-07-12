package com.taig.communicator.request;

import java.util.List;
import java.util.Map;

public class Response<T>
{
	protected int code;

	protected String message;

	protected Map<String, List<String>> headers;

	protected T result;

	public Response( int code, String message, Map<String, List<String>> headers, T result )
	{
		this.code = code;
		this.message = message;
		this.headers = headers;
		this.result = result;
	}

	public int getCode()
	{
		return code;
	}

	public String getMessage()
	{
		return message;
	}

	public Map<String, List<String>> getHeaders()
	{
		return headers;
	}

	public T getPayload()
	{
		return result;
	}
}