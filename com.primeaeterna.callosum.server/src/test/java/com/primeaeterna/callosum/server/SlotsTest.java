package com.primeaeterna.callosum.server;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SlotsTest
{
    @Test
    void firstSlotIsZero()
    {
        Slots slots = new Slots();
        assertEquals(0, slots.next());
    }

    @Test
    void slotsAreConsecutiveIntegersStartingFromZero()
    {
        Slots slots = new Slots();
        for (int i = 0; i < 1e3; ++i)
        {
            assertEquals(i, slots.next());
        }
    }

    @Test
    void allotSmallestAvailableSlot()
    {
        Slots slots = new Slots();
        for (int i = 0; i < 1e3; ++i)
        {
            slots.next();
        }
        slots.put(7);
        assertEquals(7, slots.next());
        assertEquals(1e3, slots.next());
        slots.put(19);
        slots.put(1);
        assertEquals(1, slots.next());
        assertEquals(19, slots.next());
        assertEquals(1001, slots.next());
    }

    @Test
    void returningSlotLessThanOrEqualToAnyAllotedSoFarDoesNotResultInError()
    {
        Slots slots = new Slots();
        assertEquals(0, slots.next());
        assertEquals(1, slots.next());
        assertEquals(2, slots.next());
        slots.put(1);
        assertEquals(1, slots.next());
        slots.put(0);
        assertEquals(0, slots.next());
        assertEquals(3, slots.next());
    }

    @Test
    void noDuplicateSlotsAreAllocatedAcrossThreads() throws InterruptedException
    {
        final int numOfThreads = 1000;
        final int numOfSlots = 10000;
        final CountDownLatch latch = new CountDownLatch(numOfThreads);
        final Slots slots = new Slots();
        final ConcurrentHashMap<Integer, Boolean> allotted = new ConcurrentHashMap<>();
        Runnable testRunnable = () -> {
            int[] threadAllotted = new int[numOfSlots];
            for (int i = 0; i < numOfSlots; i++)
            {
                threadAllotted[i] = slots.next();
            }
            for (int i = 0; i < numOfSlots; i++)
            {
                allotted.put(threadAllotted[i], true);
            }
            latch.countDown();
        };
        for (int i = 0; i < numOfThreads; i++)
        {
            new Thread(testRunnable).start();
        }
        latch.await();
        for (int i = 0; i < numOfThreads * numOfSlots; i++)
        {
            assertTrue(allotted.get(i));
        }
    }
}
