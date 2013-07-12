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

	public static class Proxy<T>
	{
		protected Event<T> event;

		protected Executor executor = new MainThreadExecutor();

		public Event<T> getEvent()
		{
			return event;
		}

		public Proxy( Event<T> event )
		{
			this.event = event;
		}

		public void start()
		{
			update( new Runnable()
			{
				@Override
				public void run()
				{
					event.onEvent( State.START );
					event.onStart();
				}
			} );
		}

		public void cancel()
		{
			update( new Runnable()
			{
				@Override
				public void run()
				{
					event.onEvent( State.CANCEL );
					event.onCancel();
				}
			} );
		}

		public void send( final int current, final int total )
		{
			update( new Runnable()
			{
				@Override
				public void run()
				{
					event.onEvent( State.SEND );
					event.onSend( current, total );

					if( total > 0 )
					{
						event.onSend( current / total * 100 );
					}
				}
			} );
		}

		public void receive( final int current, final int total )
		{
			update( new Runnable()
			{
				@Override
				public void run()
				{
					event.onEvent( State.RECEIVE );
					event.onReceive( current, total );

					if( total > 0 )
					{
						event.onReceive( current / total * 100 );
					}
				}
			} );
		}

		public void success( final Response<T> response )
		{
			update( new Runnable()
			{
				@Override
				public void run()
				{
					event.onEvent( State.SUCCESS );
					event.onSuccess( response );
					event.onFinish();
				}
			} );
		}

		public void failure( final Throwable error )
		{
			update( new Runnable()
			{
				@Override
				public void run()
				{
					event.onEvent( State.FAILURE );
					event.onFailure( error );
					event.onFinish();
				}
			} );
		}

		protected void update( Runnable runnable )
		{
			if( event != null )
			{
				executor.execute( runnable );
			}
		}
	}
}