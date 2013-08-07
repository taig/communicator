package com.taig.communicator.sample.io;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.taig.communicator.concurrent.Communicator;
import com.taig.communicator.event.Event;
import com.taig.communicator.method.Get;
import com.taig.communicator.request.Response;
import com.taig.communicator.sample.R;
import com.taig.communicator.sample.result.ReadAndIgnore;

import java.net.URL;

import static com.taig.communicator.method.Method.GET;

public class MultipleConnections extends Interaction
{
	protected ProgressBar progressBar1;

	protected ProgressBar progressBar2;

	protected ProgressBar progressBar3;

	protected ProgressBar progressBar4;

	protected ProgressBar progressBar5;

	protected Button stop;

	protected Button cancel;

	public MultipleConnections( Context context )
	{
		super( context, View.inflate( context, R.layout.multiple_progress, null ) );
		this.progressBar1 = (ProgressBar) main.findViewById( R.id.progress_bar_multiple_connections_1 );
		this.progressBar2 = (ProgressBar) main.findViewById( R.id.progress_bar_multiple_connections_2 );
		this.progressBar3 = (ProgressBar) main.findViewById( R.id.progress_bar_multiple_connections_3 );
		this.progressBar4 = (ProgressBar) main.findViewById( R.id.progress_bar_multiple_connections_4 );
		this.progressBar5 = (ProgressBar) main.findViewById( R.id.progress_bar_multiple_connections_5 );
		this.stop = (Button) main.findViewById( R.id.button_stop );
		this.cancel = (Button) main.findViewById( R.id.button_cancel );
	}

	@Override
	public String getIdleText()
	{
		return super.getIdleText() + " (2)";
	}

	@Override
	public void interact() throws Exception
	{
		final Communicator communicator = new Communicator( 2 );

		Get one = GET( ReadAndIgnore.class, new URL( "http://vhost2.hansenet.de/1_mb_file.bin" ), new Event.Payload<Void>()
		{
			@Override
			protected void onReceive( int progress )
			{
				progressBar1.setProgress( progress );
			}

			@Override
			protected void onSuccess( Response.Payload<Void> response )
			{
				updateText();
			}
		} );

		Get two = GET( ReadAndIgnore.class, new URL( "http://vhost2.hansenet.de/1_mb_file.bin.gz" ), new Event.Payload<Void>()
		{
			@Override
			protected void onReceive( int progress )
			{
				progressBar2.setProgress( progress );
			}

			@Override
			protected void onSuccess( Response.Payload<Void> response )
			{
				updateText();
			}
		} );

		Get three = GET( ReadAndIgnore.class, new URL( "http://vhost2.hansenet.de/1_mb_file.bin" ), new Event.Payload<Void>()
		{
			@Override
			protected void onReceive( int progress )
			{
				progressBar3.setProgress( progress );
			}

			@Override
			protected void onSuccess( Response.Payload<Void> response )
			{
				updateText();
			}
		} );

		Get four = GET( ReadAndIgnore.class, new URL( "http://vhost2.hansenet.de/1_mb_file.bin" ), new Event.Payload<Void>()
		{
			@Override
			protected void onReceive( int progress )
			{
				progressBar4.setProgress( progress );
			}

			@Override
			protected void onSuccess( Response.Payload<Void> response )
			{
				updateText();
			}
		} );

		Get five = GET( ReadAndIgnore.class, new URL( "http://vhost2.hansenet.de/1_mb_file.bin" ), new Event.Payload<Void>()
		{
			@Override
			protected void onReceive( int progress )
			{
				progressBar5.setProgress( progress );
			}

			@Override
			protected void onSuccess( Response.Payload<Void> response )
			{
				updateText();
			}
		} );

		communicator.execute( one );
		communicator.execute( two );
		communicator.execute( three );
		communicator.execute( four );
		communicator.execute( five );

		stop.setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				communicator.close();
				getTextView().setText( "Stopped" );
			}
		} );

		cancel.setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				communicator.closeNow();
				getTextView().setText( "Cancelled" );
			}
		} );
	}

	protected void updateText()
	{
		TextView textView = getTextView();

		if( textView.getText().equals( getIdleText() ) )
		{
			textView.setText( "+" );
		}
		else
		{
			textView.setText( textView.getText() + "+" );
		}
	}
}