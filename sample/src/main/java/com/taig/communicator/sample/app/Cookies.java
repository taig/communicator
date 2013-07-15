package com.taig.communicator.sample.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import com.taig.communicator.request.CookieStore;
import com.taig.communicator.request.Response;
import com.taig.communicator.result.Text;
import com.taig.communicator.sample.R;

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
					Response<Void> response = HEAD( "https://github.com" ).request();
					CookieStore store = new CookieStore( Cookies.this );
					store.add( response );

					final String manual = GET( Text.class, "http://httpbin.org/get" )
							.setCookies( response.getCookies() )
							.request()
							.getPayload();

					final String automatic = POST( Text.class, "http://httpbin.org/post", null )
							.setCookies( store )
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