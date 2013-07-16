package com.taig.communicator.result;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Text implements Parser<String>
{
	@Override
	public String parse( URL url, InputStream stream ) throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];

		try
		{
			for( int length = 0; length != -1; length = stream.read( buffer ) )
			{
				bytes.write( buffer, 0, length );
			}

			return new String( bytes.toByteArray() );
		}
		finally
		{
			bytes.close();
		}
	}
}