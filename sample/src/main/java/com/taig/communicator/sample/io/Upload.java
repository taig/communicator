package com.taig.communicator.sample.io;

import android.content.Context;
import android.text.Html;
import com.taig.communicator.event.Event;
import com.taig.communicator.data.Data;
import com.taig.communicator.data.Parameter;
import com.taig.communicator.result.Text;

import static com.taig.communicator.method.Method.POST;

public class Upload extends Interaction
{
	public Upload( Context context )
	{
		super( context );
	}

	@Override
	public void interact() throws Exception
	{
		Data.Multipart data = new Data.Multipart.Builder()
			.addTextFile( "upfile", "File.txt", context.getAssets().openFd( "File.txt" ), "utf-8" )
			.addParameter( new Parameter( "note", "This is a test upload from Communicator/Android" ) )
			.build();

		POST( Text.class, "http://cgi-lib.berkeley.edu/ex/fup.cgi", data, new Event.Payload<String>()
		{
			@Override
			protected void onSuccess( String payload )
			{
				getTextView().setText( Html.fromHtml( payload ) );
			}

			@Override
			protected void onFailure( Throwable error )
			{
				getTextView().setText( error.getLocalizedMessage() );
			}
		} ).run();
	}
}