package com.taig.communicator.sample.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.taig.communicator.sample.R;

import static com.taig.communicator.sample.R.id.*;

public class Main extends Activity
{
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.main );

		findViewById( button_simple_request ).setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				startActivity( new Intent( Main.this, SimpleRequest.class ) );
			}
		} );

		findViewById( button_events ).setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				startActivity( new Intent( Main.this, Events.class ) );
			}
		} );

		findViewById( button_custom_result_parser ).setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				startActivity( new Intent( Main.this, CustomResultParser.class ) );
			}
		} );

		findViewById( button_form_send ).setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				startActivity( new Intent( Main.this, FormSend.class ) );
			}
		} );

		findViewById( button_cookies ).setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				startActivity( new Intent( Main.this, Cookies.class ) );
			}
		} );
	}
}