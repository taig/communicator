package com.taig.communicator.sample.io;

import android.content.Context;
import com.taig.communicator.event.Event;
import com.taig.communicator.sample.result.Headline;

import java.net.URL;

import static com.taig.communicator.method.Method.GET;

public class CustomResultParser extends Interaction
{
	public CustomResultParser( Context context )
	{
		super( context );
	}

	@Override
	public void interact() throws Exception
	{
		GET( new Headline(), new URL( "http://stackoverflow.com" ), new Event.Payload<String>()
		{
			@Override
			protected void onSuccess( String content )
			{
				getTextView().setText( content );
			}

			@Override
			protected void onFailure( Throwable error )
			{
				getTextView().setText( error.getMessage() );
			}
		} ).run();
	}
}
