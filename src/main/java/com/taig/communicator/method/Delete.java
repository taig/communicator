package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.Stream;
import com.taig.communicator.request.Data;
import com.taig.communicator.request.Write;
import com.taig.communicator.result.Result;

import java.io.IOException;
import java.net.URL;

public class Delete<T> extends Write<T>
{
	protected Result<T> result;

	public Delete( Result<T> result, URL url, Data data, Event<T> event )
	{
		super( "DELETE", url, data, event );
		this.result = result;
	}

	@Override
	protected T read( Stream.Input input ) throws IOException
	{
		return result.process( input );
	}
}