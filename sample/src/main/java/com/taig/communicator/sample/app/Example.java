package com.taig.communicator.sample.app;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.taig.communicator.sample.io.Interaction;

public class Example extends Activity
{
	public static final String INTERACTION = Interaction.class.getName();

	protected static final String TAG = Example.class.getName();

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		try
		{
			String name = getIntent().getStringExtra( INTERACTION );

			if( name == null )
			{
				throw new IllegalArgumentException( "Send fully qualified Interaction class name with key set to Example.INTERACTION" );
			}

			final Interaction interaction = (Interaction) Class
				.forName( name )
				.getConstructor( Context.class )
				.newInstance( Example.this );

			setContentView( interaction.getMainView() );
			interaction.getTextView().setText( interaction.getIdleText() );

			AsyncTask.execute( new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						interaction.interact();
					}
					catch( Exception exception )
					{
						interaction.getTextView().setText( exception.getLocalizedMessage() );
						interaction.getTextView().setTextColor( getResources().getColor( R.color.holo_red_light ) );
						Log.e( TAG, exception.getMessage(), exception );
					}
				}
			} );
		}
		catch( Exception exception )
		{
			Toast.makeText(
				Example.this,
				"Activity creation failed; see LogCat for further explanations.",
				Toast.LENGTH_LONG ).show();
			Log.e( TAG, exception.getMessage(), exception );
			finish();
		}
	}
}