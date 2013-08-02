package com.taig.communicator.concurrent;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.*;

public class Communicator extends AbstractExecutorService
{
	protected int connections;

	protected Deque<Runnable> queue;

	public Communicator( int connections )
	{
		if( connections < 1 )
		{
			throw new IllegalArgumentException( "At least 1 connection has to be granted" );
		}

		this.connections = connections;
		this.queue = new LinkedBlockingDeque<Runnable>();
	}

	@Override
	public void shutdown()
	{
	}

	@Override
	public List<Runnable> shutdownNow()
	{
		return null;
	}

	@Override
	public boolean isShutdown()
	{
		return false;
	}

	@Override
	public boolean isTerminated()
	{
		return false;
	}

	@Override
	public boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException
	{
		return false;
	}

	@Override
	public void execute( Runnable runnable )
	{
	}

	protected class Thread extends java.lang.Thread
	{
		public Thread( Runnable runnable )
		{
			super( runnable );
		}

		@Override
		public void run()
		{
			super.run();
		}
	}
}