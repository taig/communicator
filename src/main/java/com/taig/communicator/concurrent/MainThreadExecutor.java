package com.taig.communicator.concurrent;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * An {@link Executor} that allows to execute on Android's UI-thread.
 * 
 * @see #EXECUTOR
 */
public class MainThreadExecutor implements Executor
{
	public static final Executor EXECUTOR = new MainThreadExecutor();

	private Handler handler = new Handler( Looper.getMainLooper() );

	@Override
	public void execute( Runnable runnable )
	{
		handler.post( runnable );
	}
}