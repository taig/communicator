package com.taig.communicator.result;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Image implements Parser<Bitmap>
{
	@Override
	public Bitmap parse( URL url, InputStream stream ) throws IOException
	{
		return BitmapFactory.decodeStream( stream );
	}
}