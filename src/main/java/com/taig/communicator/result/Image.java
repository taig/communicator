package com.taig.communicator.result;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;

public class Image extends Result<Bitmap>
{
	@Override
	public Bitmap process( InputStream stream ) throws IOException
	{
		return BitmapFactory.decodeStream( stream );
	}
}