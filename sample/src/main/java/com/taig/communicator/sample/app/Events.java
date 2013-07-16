package com.taig.communicator.sample.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.taig.communicator.event.Event;
import com.taig.communicator.event.State;
import com.taig.communicator.method.Get;
import com.taig.communicator.result.Text;
import com.taig.communicator.sample.R;

import java.io.InterruptedIOException;
import java.net.MalformedURLException;

import static com.taig.communicator.method.Method.GET;

public class Events extends Activity
{
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.progress );

		final TextView text = (TextView) findViewById( R.id.text );
		text.setText( "IDLE" );

		final ProgressBar progressBar = (ProgressBar) findViewById( R.id.progress_bar_events );

		final Button cancel = (Button) findViewById( R.id.button_cancel );

		try
		{
			final Get<String> request = GET( Text.class, "http://www.ourairports.com/data/airport-frequencies.csv", new Event<String>()
			{
				@Override
				protected void onEvent( State state )
				{
					if( state != State.RECEIVE && state != State.FAILURE )
					{
						text.setText( state.toString() );
					}
				}

				@Override
				protected void onCancel( InterruptedIOException exception )
				{
					text.setText( "My work here is done )-:" );
				}

				@Override
				protected void onReceive( int current, int total )
				{
					text.setText( "RECEIVE (" + current + " / " + total + " kB)" );
				}

				@Override
				protected void onReceive( int progress )
				{
					progressBar.setProgress( progress );
				}
			} );

			cancel.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick( View view )
				{
					request.cancel();
				}
			} );

			AsyncTask.execute( new Runnable()
			{
				@Override
				public void run()
				{
					request.run();
				}
			} );
		}
		catch( MalformedURLException exception )
		{
			text.setText( "Things went horribly wrong: " + exception.getMessage() );
		}
	}
}