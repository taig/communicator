package com.taig.communicator.concurrent;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public class MainThreadExecutor implements Executor
{
	private static Handler handler = new Handler( Looper.myLooper() );

	@Override
	public void execute( Runnable runnable )
	{
		handler.post( runnable );
	}
}