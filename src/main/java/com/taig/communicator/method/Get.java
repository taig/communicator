package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.Updateable;
import com.taig.communicator.request.Read;
import com.taig.communicator.result.Parser;

import java.io.IOException;
import java.net.URL;

public class Get<T> extends Read<T>
{
	protected Parser<T> parser;

	public Get( Parser<T> parser, URL url, Event<T> event )
	{
		super( "GET", url, event );
		this.parser = parser;
	}

	@Override
	protected T read( URL url, Updateable.Input input ) throws IOException
	{
		return parser.parse( url, input );
	}
}