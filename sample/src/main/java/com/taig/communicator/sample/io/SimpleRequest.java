package com.taig.communicator.sample.io;

import android.content.Context;
import com.taig.communicator.event.Event;
import com.taig.communicator.request.Response;
import com.taig.communicator.result.Text;

import static com.taig.communicator.method.Method.GET;

public class SimpleRequest extends Interaction
{
	public SimpleRequest( Context context )
	{
		super( context );
	}

	@Override
	public void interact() throws Exception
	{
		GET(
			Text.class,
			"http://www.gutenberg.org/files/43206/43206-0.txt",
			new Event.Payload<String>()
			{
				@Override
				protected void onSuccess( Response.Payload<String> response )
				{
					getTextView().setText( String.format(
						"Code: %d\nMessage: %s\n\n%s",
						response.getCode(),
						response.getMessage(),
						response.getPayload().substring( 3034, 3498 ) ) );
				}

				@Override
				protected void onFailure( Throwable error )
				{
					getTextView().setText( error.getMessage() );
				}
			} ).run();
	}
}