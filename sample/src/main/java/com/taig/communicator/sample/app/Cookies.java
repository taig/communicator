package com.taig.communicator.sample.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.taig.communicator.event.Event;
import com.taig.communicator.request.Response;
import com.taig.communicator.sample.R;

import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.taig.communicator.method.Method.HEAD;

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
					HEAD( "https://github.com", new Event<Void>()
					{
						@Override
						protected void onSuccess( Response<Void> response )
						{
							List<HttpCookie> cookies = response.getCookies();
							StringBuilder builder = new StringBuilder();

							for( HttpCookie cookie : cookies )
							{
								builder.append( cookie ).append( "\n" );
							}

							text.setText( builder );
						}

						@Override
						protected void onFailure( Throwable error )
						{
							text.setText( error.getMessage() );
						}
					} ).run();
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