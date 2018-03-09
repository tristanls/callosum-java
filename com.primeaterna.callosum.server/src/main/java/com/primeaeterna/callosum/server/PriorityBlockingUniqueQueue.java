package com.primeaeterna.callosum.server;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PriorityBlockingUniqueQueue<E> extends PriorityBlockingQueue<E>
{
    /**
     * Default array capacity.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 11;

    /**
     * Lock used to check if element is present and add it in a single
     * transaction.
     */
    private final ReentrantLock lock;

    /**
     * Creates a {@code PriorityBlockingUniqueQueue} with the default initial
     * capacity (11) that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     */
    public PriorityBlockingUniqueQueue()
    {
        super();
        this.lock = new ReentrantLock();
    }

    /**
     * Creates a {@code PriorityBlockingUniqueQueue} with the default initial
     * capacity and whose elements are ordered according to the specified
     * comparator.
     *
     * @param comparator the comparator that will be used to order this
     *        priority queue. If {@code null}, the {@linkplain Comparable
     *        natural ordering} of the elements will be used.
     */
    public PriorityBlockingUniqueQueue(Comparator<? super E> comparator)
    {
        super(DEFAULT_INITIAL_CAPACITY, comparator);
        this.lock = new ReentrantLock();
    }

    /**
     * If the element is not already present, inserts the specified element into
     * this priority queue.
     *
     * @param e the element to add
     * @return {@code true} if collection changed as a result of the call
     * @throws ClassCastException if the specified element cannot be compared
     *         with elements currently in the priority queue according to the
     *         priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean add(final E e)
    {
        return offer(e);
    }

    /**
     * If the element is not already present, inserts the specified element into
     * this priority queue.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this queue, else
     *         {@code false}
     * @throws ClassCastException if the specified element cannot be compared
     *         with elements currently in the priority queue according to the
     *         priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean offer(final E e)
    {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            if (this.contains(e))
            {
                return false;
            }
            return super.offer(e);
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    /**
     * If the element is not already present, inserts the specified element into
     * this priority queue.
     *
     * @param e the element to add
     * @param timeout This parameter is ignored as the method never blocks
     * @param unit This parameter is ignored as the method never blocks
     * @return {@code true} if the element was added to this queue, else
     *         {@code false}
     * @throws ClassCastException if the specified element cannot be compared
     *         with elements currently in the priority queue according to the
     *         priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }

    /**
     * If the element is not already present, inserts the specified element into
     * this priority queue.
     *
     * @param e the element to add
     * @throws ClassCastException if the specified element cannot be compared
     *         with elements currently in the priority queue according to the
     *         priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public void put(final E e)
    {
        offer(e);
    }
}
