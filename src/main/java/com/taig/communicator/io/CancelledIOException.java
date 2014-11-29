package com.taig.communicator.io;

import java.io.InterruptedIOException;

public class CancelledIOException extends InterruptedIOException
{
	public CancelledIOException() {}

	public CancelledIOException( String detailMessage )
	{
		super( detailMessage );
	}

	public CancelledIOException( int bytesTransferred )
	{
		this.bytesTransferred = bytesTransferred;
	}

	public CancelledIOException( String detailMessage, int bytesTransferred )
	{
		super( detailMessage );
		this.bytesTransferred = bytesTransferred;
	}
}