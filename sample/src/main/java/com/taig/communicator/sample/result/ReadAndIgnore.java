package com.taig.communicator.sample.result;

import com.taig.communicator.result.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ReadAndIgnore implements Parser<Void>
{
	@Override
	public Void parse( URL url, InputStream stream ) throws IOException
	{
		while( stream.read() != -1 )
		{
			// I don't care. Just waste my time.
		}

		return null;
	}
}
