package com.taig.communicator.sample.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import com.taig.communicator.event.Event;
import com.taig.communicator.event.State;
import com.taig.communicator.result.Text;
import com.taig.communicator.sample.R;

import static com.taig.communicator.method.Method.GET;

public class Events extends Activity
{
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.text );

		final TextView text = (TextView) findViewById( R.id.text );
		text.setText( "IDLE" );

		AsyncTask.execute( new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Thread.sleep( 1000 );

					GET( Text.class, "http://www.gutenberg.org/cache/epub/20872/pg20872.txt", new Event<String>()
					{
						@Override
						protected void onEvent( State state )
						{
							if( state != State.RECEIVE )
							{
								text.setText( state.toString() );
							}
						}

						@Override
						protected void onReceive( int current, int total )
						{
							text.setText( "RECEIVE (" + current + "kB)" );
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