package com.taig.communicator.sample.io;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import com.taig.communicator.event.Event;
import com.taig.communicator.event.State;
import com.taig.communicator.method.Get;
import com.taig.communicator.sample.R;
import com.taig.communicator.sample.result.ReadAndIgnore;

import java.io.InterruptedIOException;
import java.net.URL;

import static com.taig.communicator.method.Method.GET;

public class Events extends Interaction
{
	protected ProgressBar progressBar;

	protected Button cancel;

	public Events( Context context )
	{
		super( context, View.inflate( context, R.layout.progress, null ) );
		this.progressBar = (ProgressBar) main.findViewById( R.id.progress_bar_events );
		this.cancel = (Button) main.findViewById( R.id.button_cancel );
	}

	@Override
	public String getIdleText()
	{
		return State.IDLE.name();
	}

	@Override
	public void interact() throws Exception
	{
		final Get<Void> request = GET( ReadAndIgnore.class, new URL( "http://vhost2.hansenet.de/1_mb_file.bin" ), new Event.Payload<Void>()
		{
			@Override
			protected void onEvent( State state )
			{
				if( state != State.RECEIVE && state != State.FAILURE )
				{
					getTextView().setText( state.toString() );
				}
			}

			@Override
			protected void onCancel( InterruptedIOException exception )
			{
				getTextView().setText( "My work here is done )-:" );
			}

			@Override
			protected void onReceive( long current, long total )
			{
				getTextView().setText( "RECEIVE (" + current + " / " + total + " kB)" );
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

		request.run();
	}
}
