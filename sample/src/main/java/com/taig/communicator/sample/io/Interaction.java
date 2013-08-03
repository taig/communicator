package com.taig.communicator.sample.io;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.taig.communicator.sample.R;

public abstract class Interaction
{
	protected Context context;

	protected View main;

	public Interaction( Context context )
	{
		this( context, View.inflate( context, R.layout.text, null ) );
	}

	public Interaction( Context context, View main )
	{
		this.context = context;
		this.main = main;
	}

	public View getMainView()
	{
		return main;
	}

	public TextView getTextView()
	{
		return (TextView) main.findViewById( R.id.text );
	}

	public String getIdleText()
	{
		return "Loading ...";
	}

	public abstract void interact() throws Exception;
}