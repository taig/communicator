package com.taig.communicator.concurrent;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

public class Communicator extends AbstractExecutorService
{
	@Override
	public void shutdown()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Runnable> shutdownNow()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isShutdown()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isTerminated()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void execute( Runnable command )
	{
		throw new UnsupportedOperationException();
	}
}