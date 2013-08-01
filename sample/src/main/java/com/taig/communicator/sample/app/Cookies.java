package com.taig.communicator.sample.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import com.taig.communicator.request.PersistedCookieStore;
import com.taig.communicator.request.Response;
import com.taig.communicator.result.Text;
import com.taig.communicator.sample.R;

import java.net.HttpCookie;

import static com.taig.communicator.method.Method.*;

public class Cookies extends Activity
{
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.text );

		final TextView text = (TextView) findViewById( R.id.text );
		text.setText( "Loading ..." );

		AsyncTask.execute( new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Response response = HEAD( "http://httpbin.org/cookies/set?user=Taig&pass=strawberries" ).request();
					PersistedCookieStore store = new PersistedCookieStore( Cookies.this );
					store.add( response );
					store.add( null, "global", "cookie" );

					final String manual = GET( Text.class, "http://httpbin.org/get" )
							.putCookie( response )
							.addCookie( new HttpCookie( "local", "cookie" ) )
							.request()
							.getPayload();

					final String automatic = GET( Text.class, "http://httpbin.org/get" )
							.putCookie( store )
							.request()
							.getPayload();

					runOnUiThread( new Runnable()
					{
						@Override
						public void run()
						{
							text.setGravity( Gravity.LEFT );
							text.setText( "Request1:\n" + manual + "\n\nRequest2:\n" + automatic );
						}
					} );
				}
				catch( final Exception exception )
				{
					runOnUiThread( new Runnable()
					{
						@Override
						public void run()
						{
							text.setText( "Things went horribly wrong: " + exception.getMessage() );
						}
					} );
				}
			}
		} );
	}
}