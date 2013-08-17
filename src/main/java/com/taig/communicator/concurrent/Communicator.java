package com.taig.communicator.concurrent;

import android.app.Application;
import com.taig.communicator.io.Cancelable;
import com.taig.communicator.request.Request;

import java.net.CookiePolicy;
import java.net.CookieStore;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * An implementation of {@link Executor} designed to execute {@link Request} objects concurrently. Constructing a
 * Communicator requires to specify a maximum amount of concurrent connections, it will then spawn an equal amount of
 * {@link Thread Threads} responsible to execute incoming Requests.
 * <p/>
 * An Executor that manages several connections can be a very memory intensive construct. Therefore it is recommended to
 * not have more than one instance created during an application's runtime. A common usage pattern is a static reference
 * in the {@link Application} class.
 */
public class Communicator implements Executor, Cancelable
{
	private QueuedPool<Wrapper> pool;

	private Task[] tasks;

	private boolean closed = false;

	private CookiePolicy policy = CookiePolicy.ACCEPT_NONE;

	private CookieStore store;

	/**
	 * Construct a {@link Communicator}
	 *
	 * @param connections The maximum amount of concurrently active tasks.
	 * @throws IllegalArgumentException If the given amount of <code>connections</code> are <code>< 1</code>
	 */
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
	 * Retrieve the {@link Communicator Communicator's} {@link CookiePolicy}.
	 *
	 * @return The Communicator's CookiePolicy.
	 */
	public CookiePolicy getCookiePolicy()
	{
		return policy;
	}

	/**
	 * Retrieve the {@link Communicator Communicator's} {@link CookieStore}.
	 *
	 * @return The Communicator's CookieStore. May be <code>null</code>.
	 */
	public CookieStore getCookieStore()
	{
		return store;
	}

	/**
	 * Enable {@link Communicator Communicator's} cookie management. All accepted cookies will be passed along with
	 * following requests (if the hosts matches).
	 *
	 * @param store  The {@link CookieStore} used to store cookies. May be <code>null</code>, if the policy does not
	 *               permit cookies.
	 * @param policy The {@link CookiePolicy} to consult when a cookie is retrieved. May not be <code>null</code>.
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

	/**
	 * Check whether the {@link Communicator} has previously been closed or not.
	 *
	 * @return <code>true</code> if the Communicator has previously been closed, <code>false</code> otherwise.
	 * @see #close()
	 * @see #closeNow()
	 */
	public boolean isClosed()
	{
		return closed;
	}

	/**
	 * Remove all queued elements but do not cancel active executions.
	 * <p/>
	 * This method terminates all active Threads after they finished execution. Calling {@link #execute(Runnable)} after
	 * a call to <code>close()</code> will throw a {@link RejectedExecutionException}.
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
	 * This method terminates all active Threads as soon as possible. Calling {@link #execute(Runnable)} after a call to
	 * <code>closeNow()</code> will throw a {@link RejectedExecutionException}.
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

	/**
	 * Checks whether the {@link Communicator} has been closed and all its {@link Thread Threads} stopped execution.
	 *
	 * @return <code>true</code> if the Communicator has been closed and all its Threads stopped execution,
	 * <code>false</code> otherwise.
	 */
	public boolean isTerminated()
	{
		if( isClosed() )
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

		return false;
	}

	/**
	 * Execute a {@link Runnable} object concurrently as soon as there is room in the underlying {@link QueuedPool}.
	 *
	 * @param runnable The Runnable to execute concurrently.
	 */
	@Override
	public void execute( Runnable runnable )
	{
		execute( runnable, false );
	}

	/**
	 * Execute a {@link Runnable} object concurrently as soon as there is room in the underlying {@link QueuedPool}.
	 *
	 * @param runnable  The Runnable to execute concurrently.
	 * @param skipQueue <code>true</code> to place the Runnable at the queue's head if there is no room available in the
	 *                  pool. <code>false</code> to append the Runnable to the queue's tail.
	 */
	@SuppressWarnings( "unchecked" )
	public void execute( Runnable runnable, boolean skipQueue )
	{
		if( isClosed() )
		{
			throw new RejectedExecutionException( "Communicator has already been closed" );
		}

		pool.add( Wrapper.newInstance( runnable, store, policy ), skipQueue );
	}

	/**
	 * An internal extension of Java's {@link Thread} that manages {@link Communicator Communicator's} {@link Runnable}
	 * execution.
	 */
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