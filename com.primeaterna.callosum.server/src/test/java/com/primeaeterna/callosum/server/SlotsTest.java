package com.primeaeterna.callosum.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void returningNegativeSlotResultsInError()
    {
        Slots slots = new Slots();
        assertThrows(InvalidSlot.class, () -> slots.put(-10), "Invalid slot: -10");
    }

    @Test
    void returningSlotGreaterThanAnyAllotedSoFarResultsInError()
    {
        Slots slots = new Slots();
        assertThrows(InvalidSlot.class, () -> slots.put(1), "Invalid slot: 1");
    }

    @Test
    void returningSlotLessThanOrEqualToAnyAllotedSoFarDoesNotResultInError()
    {
        Slots slots = new Slots();
        assertEquals(0, slots.next());
        assertEquals(1, slots.next());
        assertEquals(2, slots.next());
        slots.put(1);
        slots.put(1);
        slots.put(1);
        assertEquals(1, slots.next());
        slots.put(0);
        slots.put(0);
        assertEquals(0, slots.next());
        assertEquals(3, slots.next());
    }
}
