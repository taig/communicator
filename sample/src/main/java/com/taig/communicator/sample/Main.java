package com.taig.communicator.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity
{
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.main );

		findViewById( R.id.button_simple_request ).setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				startActivity( new Intent( Main.this, SimpleRequest.class ) );
			}
		} );

		findViewById( R.id.button_events ).setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				startActivity( new Intent( Main.this, Events.class ) );
			}
		} );

		findViewById( R.id.button_custom_result_parser ).setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				Toast.makeText( Main.this, "TODO", Toast.LENGTH_SHORT ).show();
			}
		} );
	}
}