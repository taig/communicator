package com.taig.communicator.sample.io;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;
import com.taig.communicator.event.Event;
import com.taig.communicator.result.Parser;
import com.taig.communicator.sample.R;

import java.net.URL;

import static com.taig.communicator.method.Method.GET;

public class Image extends Interaction
{
	protected ImageView image;

	public Image( Context context )
	{
		super( context, R.layout.image );
		this.image = (ImageView) main.findViewById( R.id.image );
	}

	@Override
	public void interact() throws Exception
	{
		GET( Parser.IMAGE, new URL( "http://minionslovebananas.com/images/check-in-minion.jpg" ), new Event.Payload<Bitmap>()
		{
			@Override
			protected void onSuccess( Bitmap bitmap )
			{
				getTextView().setText( "(-:" );
				image.setImageBitmap( bitmap );
			}

			@Override
			protected void onFailure( Throwable error )
			{
				getTextView().setText( error.getMessage() );
			}
		} ).run();
	}
}