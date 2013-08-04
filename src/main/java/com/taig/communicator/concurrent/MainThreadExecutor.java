package com.taig.communicator.concurrent;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

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