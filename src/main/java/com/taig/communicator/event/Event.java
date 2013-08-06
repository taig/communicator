package com.taig.communicator.event;

import com.taig.communicator.request.Response;

import java.io.InterruptedIOException;

import static com.taig.communicator.concurrent.MainThreadExecutor.EXECUTOR;

public abstract class Event<R extends Response>
{
	public Proxy getProxy()
	{
		return new Proxy();
	}

	protected void onEvent( State state ) {}

	protected void onStart() {}

	protected void onCancel( InterruptedIOException exception ) {}

	protected void onSend( long current, long total ) {}

	protected void onSend( int progress ) {}

	protected void onReceive( long current, long total ) {}

	protected void onReceive( int progress ) {}

	protected void onSuccess( R response ) {}

	protected void onFailure( Throwable error ) {}

	protected void onFinish() {}

	public static abstract class Payload<T> extends Event<Response.Payload<T>>
	{
		@Override
		public Proxy getProxy()
		{
			return new Proxy();
		}

		protected void onSuccess( T payload ) {}

		public class Proxy extends Event<Response.Payload<T>>.Proxy
		{
			@Override
			public void success( final Response.Payload<T> response )
			{
				EXECUTOR.execute( new Runnable()
				{
					@Override
					public void run()
					{
						onEvent( State.SUCCESS );
						onSuccess( response );
						onSuccess( response.getPayload() );
						onFinish();
					}
				} );
			}
		}
	}

	public class Proxy
	{
		public Event<R> getEvent()
		{
			return Event.this;
		}

		public void start()
		{
			EXECUTOR.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.START );
					onStart();
				}
			} );
		}

		public void cancel( final InterruptedIOException exception )
		{
			EXECUTOR.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.CANCEL );
					onCancel( exception );
				}
			} );
		}

		public void send( final int current, final long total )
		{
			EXECUTOR.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.SEND );
					onSend( current, total );

					if( total > 0 )
					{
						onSend( (int) ( current / (float) total * 100 ) + 1 );
					}
				}
			} );
		}

		public void receive( final int current, final long total )
		{
			EXECUTOR.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.RECEIVE );
					onReceive( current, total );

					if( total > 0 )
					{
						onReceive( (int) ( current / (float) total * 100 ) + 1 );
					}
				}
			} );
		}

		public void success( final R response )
		{
			EXECUTOR.execute( new Runnable()
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
			EXECUTOR.execute( new Runnable()
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