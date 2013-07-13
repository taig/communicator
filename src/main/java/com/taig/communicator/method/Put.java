package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.Updateable;
import com.taig.communicator.request.Data;
import com.taig.communicator.request.Write;
import com.taig.communicator.result.Result;

import java.io.IOException;
import java.net.URL;

public class Put<T> extends Write<T>
{
	protected Result<T> result;

	public Put( Result<T> result, URL url, Data data, Event<T> event )
	{
		super( "POST", url, data, event );
		this.result = result;
	}

	@Override
	protected T read( URL url, Updateable.Input input ) throws IOException
	{
		return result.process( url, input );
	}
}