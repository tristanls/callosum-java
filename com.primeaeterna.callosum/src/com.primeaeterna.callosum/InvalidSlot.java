package com.primeaeterna.callosum;

public class InvalidSlot extends Error
{
    /**
     * @param slot the invalid slot
     */
    InvalidSlot(int slot)
    {
        super(String.format("InvalidSlot: %d", slot));
    }
}
