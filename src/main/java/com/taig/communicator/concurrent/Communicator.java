package com.taig.communicator.concurrent;

import com.taig.communicator.io.Cancelable;
import com.taig.communicator.request.Request;

import java.net.*;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class Communicator implements Executor, Cancelable
{
	protected QueuedPool<Wrapper> pool;

	protected Task[] tasks;

	protected boolean closed = false;

	protected CookiePolicy policy = CookiePolicy.ACCEPT_NONE;

	protected CookieStore store;

	public Communicator( int connections )
	{
		if( connections < 1 )
		{
			throw new IllegalArgumentException( "At least 1 connection has to be granted" );
		}

		this.pool = new QueuedPool<Wrapper>( connections );
		this.tasks = new Task[connections];

		for( int i = 0; i < connections; i++ )
		{
			tasks[i] = new Task();
			tasks[i].start();
		}
	}

	/**
	 * Remove all queued elements but do not cancel active executions.
	 *
	 * @see #close()
	 */
	public void stop()
	{
		pool.clear();
	}

	/**
	 * Remove all queued elements and cancel all active executions, if possible.
	 *
	 * @return <code>true</code>
	 * @see #closeNow()
	 */
	@Override
	public boolean cancel()
	{
		stop();

		for( Wrapper element : pool.getPool() )
		{
			element.cancel();
		}

		return true;
	}

	public boolean isClosed()
	{
		return closed;
	}

	/**
	 * Remove all queued elements but do not cancel active executions.
	 * <p/>
	 * This method terminates all active Threads after they finished execution. Calling {@link #execute(Runnable)}
	 * after
	 * a <code>close()</code> will throw a {@link RejectedExecutionException}.
	 *
	 * @see #stop()
	 */
	public void close()
	{
		closed = true;
		stop();

		for( Task task : tasks )
		{
			task.close();
		}
	}

	/**
	 * Remove all queued elements and cancel all active executions.
	 * <p/>
	 * This method terminates all active Threads as soon as possible. Calling {@link #execute(Runnable)} after
	 * a <code>closeNow()</code> will throw a {@link RejectedExecutionException}.
	 *
	 * @see #cancel()
	 */
	public void closeNow()
	{
		closed = true;
		stop();

		for( Task task : tasks )
		{
			task.cancel();
		}
	}

	public boolean isTerminated()
	{
		for( Task task : tasks )
		{
			if( task.isAlive() )
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Enable Communicator's cookie management. All accepted cookies will be passed along with following requests (if
	 * the hosts matches).
	 *
	 * @param store
	 * @param policy
	 * @throws IllegalArgumentException If the supplied CookiePolicy is null.
	 */
	public void accept( CookieStore store, CookiePolicy policy )
	{
		if( policy == null )
		{
			throw new IllegalArgumentException( "CookiePolicy may not be null" );
		}

		this.store = store;
		this.policy = policy;
	}

	@Override
	public void execute( Runnable runnable )
	{
		execute( runnable, false );
	}

	@SuppressWarnings( "unchecked" )
	public void execute( Runnable runnable, boolean skipQueue )
	{
		if( isClosed() )
		{
			throw new RejectedExecutionException( "Communicator has already been closed" );
		}

		if( runnable instanceof Request )
		{
			pool.add( new Wrapper.Request( (Request) runnable, store, policy ), skipQueue );
		}
		else
		{
			pool.add( new Wrapper.Runnable( runnable ), skipQueue );
		}
	}

	protected class Task extends Thread implements Cancelable
	{
		protected Wrapper<?> element;

		protected boolean stopped = false;

		@Override
		@SuppressWarnings( "unchecked" )
		public void run()
		{
			try
			{
				while( !isClosed() && !stopped )
				{
					element = pool.promote();
					element.run();
					pool.demote( element );
					element = null;
				}
			}
			catch( InterruptedException exception )
			{
				if( element != null )
				{
					element.cancel();
					pool.demote( element );
				}
			}
		}

		@Override
		public boolean isInterrupted()
		{
			return stopped || super.isInterrupted();
		}

		/**
		 * Interrupt this thread immediately if it's blocking for an event. If it's currently executing an user object,
		 * try to cancel it.
		 *
		 * @return <code>true</code>
		 */
		@Override
		public boolean cancel()
		{
			stopped = true;

			if( element == null || !element.cancel() )
			{
				interrupt();
			}

			return true;
		}

		/**
		 * Interrupt this thread immediately if it's blocking for an event. If it's currently executing an user object,
		 * let it terminate properly.
		 */
		public void close()
		{
			stopped = true;

			if( element == null )
			{
				interrupt();
			}
		}
	}
}