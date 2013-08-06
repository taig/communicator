package com.taig.communicator.sample.io;

import android.content.Context;
import android.view.Gravity;
import com.taig.communicator.event.Event;
import com.taig.communicator.data.Data;
import com.taig.communicator.data.Parameter;
import com.taig.communicator.result.Text;

import java.net.URL;

import static com.taig.communicator.method.Method.POST;

public class FormSend extends Interaction
{
	public FormSend( Context context )
	{
		super( context );
	}

	@Override
	public void interact() throws Exception
	{
		Parameter params = new Parameter();
		params.put( "username", "taig" );
		params.put( "password", "strawberry" );
		params.put( "remember", "true" );

		POST( Text.class, new URL( "http://httpbin.org/post" ), new Data.Form( params, "utf-8" ), new Event.Payload<String>()
		{
			@Override
			protected void onSuccess( String content )
			{
				getTextView().setGravity( Gravity.LEFT | Gravity.CENTER_VERTICAL );
				getTextView().setText( content );
			}
		} ).run();
	}
}