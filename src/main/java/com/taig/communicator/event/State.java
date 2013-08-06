package com.taig.communicator.event;

/**
 * All possible states that a {@link com.taig.communicator.request.Request} can have during its lifecycle.
 */
public enum State
{
	IDLE, START, CANCEL, SEND, RECEIVE, SUCCESS, FAILURE
}