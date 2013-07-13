package com.taig.communicator.event;

import com.taig.communicator.concurrent.MainThreadExecutor;
import com.taig.communicator.request.Response;

import java.util.concurrent.Executor;

public abstract class Event<T>
{
	protected void onEvent( State state ) {}

	protected void onStart() {}

	protected void onCancel() {}

	protected void onSend( int current, int total ) {}

	protected void onSend( int progress ) {}

	protected void onReceive( int current, int total ) {}

	protected void onReceive( int progress ) {}

	protected void onSuccess( Response<T> response ) {}

	protected void onFailure( Throwable error ) {}

	protected void onFinish() {}

	public class Proxy
	{
		protected Executor executor = new MainThreadExecutor();

		public Event<T> getEvent()
		{
			return Event.this;
		}

		public void start()
		{
			executor.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.START );
					onStart();
				}
			} );
		}

		public void cancel()
		{
			executor.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.CANCEL );
					onCancel();
				}
			} );
		}

		public void send( final int current, final int total )
		{
			executor.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.SEND );
					onSend( current, total );

					if( total > 0 )
					{
						onSend( current / total * 100 );
					}
				}
			} );
		}

		public void receive( final int current, final int total )
		{
			executor.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.RECEIVE );
					onReceive( current, total );

					if( total > 0 )
					{
						onReceive( current / total * 100 );
					}
				}
			} );
		}

		public void success( final Response<T> response )
		{
			executor.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.SUCCESS );
					onSuccess( response );
					onFinish();
				}
			} );
		}

		public void failure( final Throwable error )
		{
			executor.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.FAILURE );
					onFailure( error );
					onFinish();
				}
			} );
		}
	}
}