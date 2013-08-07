package com.taig.communicator.event;

import com.taig.communicator.request.Request;

/**
 * All possible states that a {@link Request} can have during its lifecycle.
 */
public enum State
{
	IDLE, START, CONNECT, CANCEL, SEND, RECEIVE, SUCCESS, FAILURE
}