package com.taig.communicator.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class Main extends Activity
{
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.main );

		TextView text = (TextView) findViewById( R.id.textview );
		text.setText( "Hello World!" );
	}
}