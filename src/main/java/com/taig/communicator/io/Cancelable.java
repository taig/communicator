package com.taig.communicator.io;

/**
 * The Cancelable interface is used for classes that execute time-consuming tasks and adds a possibility to stop that
 * execution in a proper manner.
 */
public interface Cancelable
{
	/**
	 * Try to cancel the execution as soon as possible with the result that the planned execution may not be completed.
	 *
	 * @return <code>true</code> if the premature termination has been initiated, <code>false</code> otherwise.
	 */
	public boolean cancel();
}