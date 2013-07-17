package com.taig.communicator.sample.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import com.taig.communicator.event.Event;
import com.taig.communicator.request.Response;
import com.taig.communicator.sample.R;
import com.taig.communicator.sample.result.Headline;

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
					GET( Headline.class, "http://stackoverflow.com", new Event.Payload<String>()
					{
						@Override
						protected void onSuccess( String content )
						{
							text.setText( content );
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