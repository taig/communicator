package com.taig.communicator.concurrent;

import com.taig.communicator.io.Cancelable;
import com.taig.communicator.request.Request;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class Communicator implements Executor, Cancelable
{
	protected QueuedPool<Request> pool;

	protected Thread[] threads;

	protected boolean closed = false;

	public Communicator( int connections )
	{
		if( connections < 1 )
		{
			throw new IllegalArgumentException( "At least 1 connection has to be granted" );
		}

		this.pool = new QueuedPool<Request>( connections );
		this.threads = new Thread[connections];

		for( int i = 0; i < connections; i++ )
		{
			threads[i] = new Thread();
			threads[i].start();
		}
	}

	public void stop()
	{
		closed = true;
		pool.clear();
	}

	@Override
	public void cancel()
	{
		stop();

		for( Request request : pool.getPool() )
		{
			request.cancel();
		}
	}

	public boolean isClosed()
	{
		return closed;
	}

	public boolean isTerminated()
	{
		for( Thread thread : threads )
		{
			if( thread.isAlive() )
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public void execute( Runnable runnable )
	{
		if( runnable instanceof Request )
		{
			request( (Request) runnable );
		}
		else
		{
			throw new IllegalArgumentException( "Please provide a " + Request.class.getName() + " object" );
		}
	}

	public void request( Request request )
	{
		pool.add( request );
	}

	public void request( Request request, boolean skipQueue )
	{
		pool.add( request, skipQueue );
	}

	protected class Thread extends java.lang.Thread
	{
		protected Request request;

		@Override
		public void run()
		{
			try
			{
				while( !closed )
				{
					request = pool.promote();
					request.run();
					pool.demote( request );
					request = null;
				}
			}
			catch( InterruptedException exception )
			{
				if( request != null )
				{
					request.cancel();
				}
			}
		}
	}
}