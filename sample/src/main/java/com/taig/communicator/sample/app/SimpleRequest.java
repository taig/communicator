package com.taig.communicator.sample.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import com.taig.communicator.request.Response;
import com.taig.communicator.result.Text;
import com.taig.communicator.sample.R;

import static com.taig.communicator.method.Method.GET;

public class SimpleRequest extends Activity
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
					final Response<String> response = GET( Text.class, "http://www.gutenberg.org/files/43206/43206-0.txt" ).request();

					runOnUiThread( new Runnable()
					{
						@Override
						public void run()
						{
							text.setText(
									"Code: " + response.getCode() + "\n" +
									"Message: " + response.getMessage() + "\n\n" +
									response.getPayload().substring( 3034, 3498 ) + " ..." );
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