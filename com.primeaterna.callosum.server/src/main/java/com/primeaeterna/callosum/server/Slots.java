package com.primeaeterna.callosum.server;

import java.util.Optional;
import java.util.Queue;

/**
 * The slots manager keeps track of available slots. When asked for next
 * available slot it will always return the lowest available slot.
 */
public class Slots
{
    private int nextSlot = -1;
    private Queue<Integer> minQueue = new PriorityBlockingUniqueQueue<Integer>((a, b) -> a - b);

    /**
     * @return next available slot
     */
    public int next()
    {
        return Optional.ofNullable(this.minQueue.poll())
                       .orElseGet(() -> ++this.nextSlot);
    }

    /**
     * Returns a previously retrieved slot back to the pool.
     *
     * @param slot slot to return
     *
     * @throws {@link InvalidSlot} if the slot number is greater than the
     *                largest slot number allotted so far
     */
    public void put(int slot)
    {
        if (slot > this.nextSlot || slot < 0)
        {
            throw new InvalidSlot(slot);
        }
        this.minQueue.add(slot);
    }
}
