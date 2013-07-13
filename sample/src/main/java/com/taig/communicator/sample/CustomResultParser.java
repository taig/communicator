package com.taig.communicator.sample;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import com.taig.communicator.event.Event;
import com.taig.communicator.request.Response;
import com.taig.communicator.result.Headline;

import static com.taig.communicator.method.Method.GET;

public class CustomResultParser extends Activity
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
					GET( Headline.class, "http://stackoverflow.com", new Event<String>()
					{
						@Override
						protected void onSuccess( Response<String> response )
						{
							text.setText( response.getPayload() );
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