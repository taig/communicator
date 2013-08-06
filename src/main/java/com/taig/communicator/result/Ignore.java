package com.taig.communicator.result;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * An implementation of {@link Parser} that ignores the server's response, does not read it, and immediately returns
 * <code>null</code>.
 */
public class Ignore implements Parser<Void>
{
	/**
	 * {@inheritDoc}
	 *
	 * @return <code>null</code>
	 */
	@Override
	public Void parse( URL url, InputStream stream ) throws IOException
	{
		return null;
	}
}