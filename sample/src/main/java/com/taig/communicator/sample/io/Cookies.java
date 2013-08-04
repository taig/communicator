package com.taig.communicator.sample.io;

import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import com.taig.communicator.data.PersistedCookieStore;
import com.taig.communicator.request.Response;
import com.taig.communicator.result.Text;

import java.net.CookieStore;
import java.net.HttpCookie;

import static com.taig.communicator.method.Method.GET;
import static com.taig.communicator.method.Method.HEAD;

public class Cookies extends Interaction
{
	public Cookies( Context context )
	{
		super( context );
	}

	@Override
	public void interact() throws Exception
	{
		Response response = HEAD( "http://httpbin.org/cookies/set?user=Taig&pass=strawberries" ).request();
		CookieStore store = new PersistedCookieStore( context );

		for( HttpCookie cookie : response.getCookies() )
		{
			store.add( response.getURL().toURI(), cookie );
		}

		HttpCookie globalCookie = new HttpCookie( "global", "cookie" );
		globalCookie.setVersion( 0 );
		store.add( null, globalCookie );

		HttpCookie localCookie = new HttpCookie( "local", "cookie" );
		localCookie.setVersion( 0 );

		final String manual = GET( Text.class, "http://httpbin.org/get" )
			.putCookie( response )
			.addCookie( localCookie )
			.request()
			.getPayload();

		final String automatic = GET( Text.class, "http://httpbin.org/get" )
			.putCookie( store )
			.request()
			.getPayload();

		new Handler( context.getMainLooper() ).post( new Runnable()
		{
			@Override
			public void run()
			{
				getTextView().setGravity( Gravity.LEFT );
				getTextView().setText( "Manual Cookie Request:\n" + manual + "\n\nCookie Store Request:\n" + automatic );
			}
		} );
	}
}