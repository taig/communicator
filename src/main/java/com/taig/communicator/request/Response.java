package com.taig.communicator.request;

public class Response<T>
{
	protected T result;

	public Response( T result )
	{
		this.result = result;
	}

	public T getPayload()
	{
		return result;
	}
}