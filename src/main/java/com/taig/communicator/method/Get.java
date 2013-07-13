package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.Stream;
import com.taig.communicator.request.Read;
import com.taig.communicator.result.Result;

import java.io.IOException;
import java.net.URL;

public class Get<T> extends Read<T>
{
	protected Result<T> result;

	public Get( Result<T> result, URL url, Event<T> event )
	{
		super( "GET", url, event );
		this.result = result;
	}

	@Override
	protected T read( URL url, Stream.Input input ) throws IOException
	{
		return result.process( url, input );
	}
}