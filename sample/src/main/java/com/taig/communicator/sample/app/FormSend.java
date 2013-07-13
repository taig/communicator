package com.taig.communicator.sample.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import com.taig.communicator.request.Data;
import com.taig.communicator.request.Response;
import com.taig.communicator.result.Text;
import com.taig.communicator.sample.R;

import java.util.HashMap;
import java.util.Map;

import static com.taig.communicator.method.Method.POST;

public class FormSend extends Activity
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
					Map<String, String> params = new HashMap<String, String>();
					params.put( "username", "taig" );
					params.put( "password", "strawberry" );
					params.put( "remember", "true" );

					final Response<String> response = POST( Text.class, "http://httpbin.org/post", Data.from( params ) ).request();

					runOnUiThread( new Runnable()
					{
						@Override
						public void run()
						{
							text.setGravity( Gravity.LEFT | Gravity.CENTER_VERTICAL );
							text.setText( response.getPayload() );
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
							Log.d( "ASDF", "FUCK", exception );
						}
					} );
				}
			}
		} );
	}
}