package com.taig.communicator.result;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class Text extends Result<String>
{
	@Override
	public String process( URL url, InputStream stream ) throws IOException
	{
		Scanner scanner = new Scanner( stream, "UTF-8" ).useDelimiter( "\\A" );
		return scanner.hasNext() ? scanner.next() : "";
	}
}