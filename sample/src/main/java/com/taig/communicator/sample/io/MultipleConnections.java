package com.taig.communicator.sample.io;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import com.taig.communicator.concurrent.Communicator;
import com.taig.communicator.event.Event;
import com.taig.communicator.event.State;
import com.taig.communicator.method.Get;
import com.taig.communicator.sample.R;
import com.taig.communicator.sample.result.ReadAndIgnore;

import static com.taig.communicator.method.Method.GET;

public class MultipleConnections extends Interaction
{
	protected ProgressBar progressBar1;

	protected ProgressBar progressBar2;

	protected ProgressBar progressBar3;

	protected ProgressBar progressBar4;

	protected ProgressBar progressBar5;

	public MultipleConnections( Context context )
	{
		super( context, View.inflate( context, R.layout.multiple_progress, null ) );
		this.progressBar1 = (ProgressBar) main.findViewById( R.id.progress_bar_multiple_connections_1 );
		this.progressBar2 = (ProgressBar) main.findViewById( R.id.progress_bar_multiple_connections_2 );
		this.progressBar3 = (ProgressBar) main.findViewById( R.id.progress_bar_multiple_connections_3 );
		this.progressBar4 = (ProgressBar) main.findViewById( R.id.progress_bar_multiple_connections_4 );
		this.progressBar5 = (ProgressBar) main.findViewById( R.id.progress_bar_multiple_connections_5 );
	}

	@Override
	public void interact() throws Exception
	{
		Communicator communicator = new Communicator( 2 );

		Get<Void> one = GET( ReadAndIgnore.class, "http://vhost2.hansenet.de/1_mb_file.bin", new Event.Payload<Void>()
		{
			@Override
			protected void onReceive( int progress )
			{
				progressBar1.setProgress( progress );
			}
		} );

		Get<Void> two = GET( ReadAndIgnore.class, "http://vhost2.hansenet.de/1_mb_file.bin", new Event.Payload<Void>()
		{
			@Override
			protected void onReceive( int progress )
			{
				progressBar2.setProgress( progress );
			}
		} );

		Get<Void> three = GET( ReadAndIgnore.class, "http://vhost2.hansenet.de/1_mb_file.bin", new Event.Payload<Void>()
		{
			@Override
			protected void onReceive( int progress )
			{
				progressBar3.setProgress( progress );
			}
		} );

		Get<Void> four = GET( ReadAndIgnore.class, "http://vhost2.hansenet.de/1_mb_file.bin", new Event.Payload<Void>()
		{
			@Override
			protected void onReceive( int progress )
			{
				progressBar4.setProgress( progress );
			}
		} );

		Get<Void> five = GET( ReadAndIgnore.class, "http://vhost2.hansenet.de/1_mb_file.bin", new Event.Payload<Void>()
		{
			@Override
			protected void onReceive( int progress )
			{
				progressBar5.setProgress( progress );
			}
		} );

		communicator.request( one );
		communicator.request( two );
		communicator.request( three );
		communicator.request( four );
		communicator.request( five );
	}
}