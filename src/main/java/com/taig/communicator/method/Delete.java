package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.Updateable;
import com.taig.communicator.request.Data;
import com.taig.communicator.request.Write;
import com.taig.communicator.result.Parser;

import java.io.IOException;
import java.net.URL;

import static com.taig.communicator.method.Method.*;

public class Delete<T> extends Write<T>
{
	protected Parser<T> parser;

	public Delete( Parser<T> parser, URL url, Data data, Event.Payload<T> event )
	{
		super( Type.DELETE, url, data, event );
		this.parser = parser;
	}

	@Override
	protected T read( URL url, Updateable.Input input ) throws IOException
	{
		return parser.parse( url, input );
	}
}