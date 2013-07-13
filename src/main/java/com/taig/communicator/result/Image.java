package com.taig.communicator.result;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Image extends Result<Bitmap>
{
	@Override
	public Bitmap process( URL url, InputStream stream ) throws IOException
	{
		return BitmapFactory.decodeStream( stream );
	}
}