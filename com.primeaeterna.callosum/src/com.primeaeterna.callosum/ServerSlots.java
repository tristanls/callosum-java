package com.primeaeterna.callosum;

import java.util.Optional;

/**
 * The slot manager keeps track of available slots and when asked for next
 * available slot it will always return the lowest available slot.
 */
public class ServerSlots
{
    private int nextSlot = -1;
    private PriorityBlockingUniqueQueue<Integer> minQueue = new PriorityBlockingUniqueQueue<Integer>((a, b) -> a - b);

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
