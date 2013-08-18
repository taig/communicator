package com.taig.communicator.concurrent;

import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * A threadsafe {@link Collection} implementation that is designed for concurrent executions featuring a limited size
 * pool and a preceding {@link Queue}.
 * <p/>
 * If an element is added to the QueuedPool it will first be placed in the pool's queue. The queue's head can be moved
 * into the pool by calling {@link #promote()} (blocks until there is room available in the pool). An element can be
 * removed from the pool by calling {@link #demote(Object)}.
 *
 * @param <T> The Collection's type.
 */
public class QueuedPool<T> implements Collection<T>
{
	private int size;

	private Deque<T> queue = new LinkedList<T>();

	private Collection<T> pool = new ArrayList<T>();

	private Semaphore semaphore;

	public QueuedPool( int size )
	{
		if( size < 1 )
		{
			throw new IllegalArgumentException( "Pool size must be greater than 0" );
		}

		this.size = size;
		this.semaphore = new Semaphore( size );
	}

	/**
	 * Retrieve a copy of the current queue.
	 *
	 * @return A copy of the current queue.
	 */
	public synchronized List<T> getQueue()
	{
		return new ArrayList<T>( queue );
	}

	/**
	 * Retrieve a copy of the current pool.
	 *
	 * @return A copy of the current pool.
	 */
	public synchronized Collection<T> getPool()
	{
		return new ArrayList<T>( pool );
	}

	/**
	 * Retrieve a collection that contains all objects from queue and pool in no specific order.
	 *
	 * @return A collection that contains all object from queue and pool.
	 */
	public synchronized Collection<T> getAll()
	{
		ArrayList<T> all = new ArrayList<T>( size() );
		all.addAll( queue );
		all.addAll( pool );
		return all;
	}

	/**
	 * Retrieve the pool's maximum capacity.
	 *
	 * @return The pool's maximum capacity.
	 */
	public int poolSize()
	{
		return size;
	}

	/**
	 * Retrieve the amount of active and queued items combined.
	 *
	 * @return The amount of active and queued items combined.
	 */
	@Override
	public synchronized int size()
	{
		return queue.size() + pool.size();
	}

	/**
	 * Check if no item is queued or stored in the pool.
	 *
	 * @return <code>true</code> if no item is queued or stored in the pool, <code>false</code> otherwise.
	 */
	@Override
	public synchronized boolean isEmpty()
	{
		return queue.isEmpty() && pool.isEmpty();
	}

	/**
	 * Check if the given object exists within the queue or the pool.
	 *
	 * @param object The object to check whether it exists or not.
	 * @return <code>true</code> if the given object exists within the queue or the pool, <code>false</code> otherwise.
	 */
	@Override
	public synchronized boolean contains( Object object )
	{
		return queue.contains( object ) || queue.contains( object );
	}

	/**
	 * Iterate over a copy of the current dataset.
	 * <p/>
	 * The underlying dataset combines queue and pool data in no particual order.
	 *
	 * @return The iterator of a copy of the current dataset.
	 * @see #getAll()
	 */
	@Override
	public Iterator<T> iterator()
	{
		return getAll().iterator();
	}

	/**
	 * Create an array representation of the queue and pool data in no particular order.
	 *
	 * @return The array representation of the queue and pool data.
	 */
	@Override
	public Object[] toArray()
	{
		return getAll().toArray();
	}

	/**
	 * Create a typed array representation of the queue and pool data in no particular order.
	 *
	 * @param array The array into which the elements of this collection are to be stored, if it is big enough;
	 *              otherwise, a new array of the same runtime type is allocated for this purpose.
	 * @return The typed array representation of the queue and pool data.
	 */
	@Override
	public <A> A[] toArray( A[] array )
	{
		return getAll().toArray( array );
	}

	/**
	 * Add an element to the queue; it may be promoted to the pool later on.
	 *
	 * @param element The element to add to the queue.
	 * @return <code>true</code>
	 */
	@Override
	public boolean add( T element )
	{
		return add( element, false );
	}

	/**
	 * Add an element to the queue; it may be promoted to the pool later on.
	 *
	 * @param element   The element to add to the queue.
	 * @param skipQueue <code>false</code> to append the element to the queue, <code>true</code> to place the element
	 *                  at
	 *                  the queue's head.
	 * @return <code>true</code>
	 */
	public synchronized boolean add( T element, boolean skipQueue )
	{
		if( skipQueue )
		{
			queue.offerFirst( element );
		}
		else
		{
			queue.offerLast( element );
		}

		notify();
		return true;
	}

	/**
	 * Remove the first occurrence of the given object from the queue.
	 *
	 * @param object The object to remove from the queue.
	 * @return <code>true</code> if the given object has been removed from the queue, <code>false</code> if it did not
	 * exist.
	 */
	@Override
	public synchronized boolean remove( Object object )
	{
		return queue.remove( object );
	}

	/**
	 * Check if the given objects exist within the queue or the pool.
	 *
	 * @param objects The objects to check whether they exist or not.
	 * @return <code>true</code> if the given objects exist within the queue or the pool, <code>false</code> otherwise.
	 */
	@Override
	public boolean containsAll( Collection<?> objects )
	{
		return getAll().contains( objects );
	}

	/**
	 * Add all elements to the queue; they may be promoted to the pool later on.
	 *
	 * @param elements The elements to add to the queue.
	 * @return <code>true</code> if the queue changed as a result of the call, <code>false</code> otherwise.
	 */
	@Override
	public synchronized boolean addAll( Collection<? extends T> elements )
	{
		queue.addAll( elements );
		notifyAll();
		return true;
	}

	/**
	 * Removes all elements from the queue that are also contained in the given collection. After this call returns,
	 * the
	 * queue will contain no elements in common with the given collection.
	 *
	 * @param objects Collection containing elements to be removed from the queue.
	 * @return <code>true</code> if the queue changed as a result of the call, <code>false</code> otherwise.
	 */
	@Override
	public synchronized boolean removeAll( Collection<?> objects )
	{
		return queue.removeAll( objects );
	}

	/**
	 * Retains only the elements in the queue that are contained in the given collection. In other words, removes from
	 * the queue all of its elements that are not contained in the given collection.
	 *
	 * @param objects Collection containing elements to be retained in the queue.
	 * @return <code>true</code> if the queue changed as a result of the call, <code>false</code> otherwise.
	 */
	@Override
	public synchronized boolean retainAll( Collection<?> objects )
	{
		return queue.retainAll( objects );
	}

	/**
	 * Removes all of the elements from the queue.
	 */
	@Override
	public void clear()
	{
		queue.clear();
	}

	/**
	 * Promote the head of the queue to be moved to the pool.
	 * <p/>
	 * This method blocks until ...
	 * <ul>
	 * <li>... the pool has room to accept the element.</li>
	 * <li>... the queue has an element available.</li>
	 * </ul>
	 *
	 * @return The element that has been promoted from queue to pool.
	 */
	public T promote() throws InterruptedException
	{
		// Wait for pool to have room available.
		semaphore.acquire();

		synchronized( this )
		{
			while( queue.isEmpty() )
			{
				// Wait until an element is available in the queue.
				wait();
			}

			T element = queue.poll();
			pool.add( element );
			return element;
		}
	}

	/**
	 * Demote an object from the pool and thereby remove it from the collection.
	 *
	 * @param object The object to remove from the pool.
	 * @return <code>true</code> if the pool changed as a result of the call, <code>false</code> otherwise.
	 */
	public synchronized boolean demote( Object object )
	{
		if( pool.remove( object ) )
		{
			semaphore.release();
			return true;
		}
		else
		{
			return false;
		}
	}
}