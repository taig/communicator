package com.taig.communicator.result;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Ignore implements Parser<Void>
{
	public static final Ignore PARSER = new Ignore();

	@Override
	public Void parse( URL url, InputStream stream ) throws IOException
	{
		return null;
	}
}