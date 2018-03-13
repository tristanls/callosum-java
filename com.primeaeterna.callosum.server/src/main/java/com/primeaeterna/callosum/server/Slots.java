package com.primeaeterna.callosum.server;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The slots manager keeps track of available slots. When asked for next
 * available slot it will always return the lowest available slot starting
 * with zero.
 */
public class Slots
{
    private AtomicInteger nextSlot = new AtomicInteger(-1);
    private Queue<Integer> minQueue = new PriorityBlockingQueue<Integer>();

    /**
     * @return next available slot
     */
    public int next()
    {
        return Optional.ofNullable(this.minQueue.poll())
                       .orElseGet(() -> this.nextSlot.addAndGet(1));
    }

    /**
     * Returns a previously retrieved slot back to the pool.
     *
     * No domain or uniqueness checks are necessary as long as the previously
     * retrieved slot has been treated as an immutable opaque token.
     *
     * @param slot slot to return
     */
    public void put(int slot)
    {
        this.minQueue.offer(slot);
    }
}
