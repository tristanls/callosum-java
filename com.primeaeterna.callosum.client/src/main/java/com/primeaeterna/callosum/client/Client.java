package com.primeaeterna.callosum.client;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.Comparator;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.ToIntFunction;

public class Client
{
    private static final AttributeKey<Boolean> CALLOSUM_CLOSING = AttributeKey.valueOf("callosum.closing");
    private static final AttributeKey<Boolean> CALLOSUM_LEASE = AttributeKey.valueOf("callosum.lease");
    private static final AttributeKey<Integer> CALLOSUM_SLOT = AttributeKey.valueOf("callosum.slot");

    private static final int DEFAULT_INITIAL_CAPACITY = 11;

    private static final int DEFAULT_MAX_CHANNELS = 100;

    private AtomicBoolean canEnter = new AtomicBoolean(true);

    private final int capacity;

    /**
     * Min heap is maintained to quickly provide channel with minimal slot
     * number on request.
     */
    private Queue<Channel> minHeap = new PriorityBlockingQueue<>(
            DEFAULT_INITIAL_CAPACITY,
            Comparator.comparingInt(ch -> ch.attr(CALLOSUM_SLOT).get())
    );

    /**
     * Max heap is maintained to quickly check if a newly available channel
     * has lower slot number than highest slot number channel. In which case,
     * new channel would be added and highest slot number channel would be
     * discarded.
     */
    private Queue<Channel> maxHeap = new PriorityBlockingQueue<>(
            DEFAULT_INITIAL_CAPACITY,
            Comparator.comparingInt((ToIntFunction<Channel>) ch -> ch.attr(CALLOSUM_SLOT).get()).reversed()
    );

    private int slotCount = 0;

    public Client()
    {
        this.capacity = DEFAULT_MAX_CHANNELS;
    }

    /**
     *
     * @param maxChannels maximum number of channels to maintain
     */
    public Client(int maxChannels)
    {
        if (maxChannels < 1)
        {
            throw new IllegalArgumentException();
        }
        this.capacity = maxChannels;
    }

    /**
     *
     * @return
     */
    public Channel acquire()
    {
        while (canEnter.compareAndSet(true, false))
        {
            Thread.onSpinWait();
        }
        try
        {
            return unsafeAcquire();
        }
        finally
        {
            canEnter.set(true); // doubles as memory barrier for unsafeAcquire()
        }
    }

    /**
     *
     * @param channel
     */
    public void newChannel(Channel channel)
    {
        if (!channel.hasAttr(CALLOSUM_SLOT))
        {
            throw new IllegalArgumentException();
        }
        while (canEnter.compareAndSet(true, false))
        {
            Thread.onSpinWait();
        }
        try
        {
            unsafeNewChannel(channel);
        }
        finally
        {
            canEnter.set(true); // doubles as memory barrier for unsafeNewChannel()
        }
    }

    /**
     *
     * @param channel
     */
    public void release(Channel channel)
    {
        while (canEnter.compareAndSet(true, false))
        {
            Thread.onSpinWait();
        }
        try
        {
            unsafeRelease(channel);
        }
        finally
        {
            canEnter.set(true); // doubles as memory barrier for unsafeRelease()
        }
    }

    /**
     * Not thread safe implementation of {@link #acquire()} that should be gated
     * to single thread access only, followed by memory barrier.
     */
    private Channel unsafeAcquire()
    {
        Channel minChannel = this.minHeap.poll();
        // Opportunistically empty closed channels
        while (minChannel != null
               && (!minChannel.isOpen()
                   || Optional.ofNullable(minChannel.attr(CALLOSUM_CLOSING).get()).orElse(false)))
        {
            minChannel = this.minHeap.poll();
        }

        if (minChannel != null)
        {
            minChannel.attr(CALLOSUM_LEASE).set(true);
        }

        return minChannel;
    }

    /**
     * Not thread safe implementation of {@link #newChannel(Channel)} that
     * should be gated to single thread access only, followed by memory barrier.
     *
     * @param channel
     */
    private void unsafeNewChannel(Channel channel)
    {
        if (this.slotCount < this.capacity)
        {
            unsafeInsertChannel(channel);
            return;
        }

        // We are at maximum capacity, look to replace existing channel.
        Channel maxChannel = this.maxHeap.peek();
        // Opportunistically empty closed channels
        boolean emptiedClosedChannel = false;
        while (maxChannel != null
               && (!maxChannel.isOpen()
                   || Optional.ofNullable(maxChannel.attr(CALLOSUM_CLOSING).get()).orElse(false)))
        {
            this.maxHeap.poll();
            emptiedClosedChannel = true;
            maxChannel = this.maxHeap.peek();
        }

        // Opportunistically flex capacity and accept new channel as we have
        // removed some that will be asynchronously accounted for later via
        // close future.
        if (emptiedClosedChannel)
        {
            unsafeInsertChannel(channel);
            return;
        }

        if (maxChannel == null)
        {
            // We are at maximum capacity and there are no open channels?
            // Close and discard new channel.
            // This should be dead code, prove it.
            channel.close();
            return;
        }

        // If new channel isn't lower slot number, close and discard it.
        if (maxChannel.attr(CALLOSUM_SLOT).get() <= channel.attr(CALLOSUM_SLOT).get())
        {
            channel.close();
            return;
        }

        maxChannel = this.maxHeap.poll();
        maxChannel.attr(CALLOSUM_CLOSING).set(true);

        if (!Optional.ofNullable(maxChannel.attr(CALLOSUM_LEASE).get()).orElse(false))
        {
            maxChannel.close();
        }

        unsafeInsertChannel(channel);
    }

    /**
     * Not thread safe insertion of channel into the min and max heaps for
     * slot number tracking.
     *
     * @param channel channel to insert
     */
    private void unsafeInsertChannel(final Channel channel)
    {
        channel.closeFuture().addListener((f) ->
            {
                while (canEnter.compareAndSet(true, false))
                {
                    Thread.onSpinWait();
                }
                try
                {
                    this.minHeap.remove(channel);
                    this.maxHeap.remove(channel);
                    this.slotCount--;
                }
                finally
                {
                    canEnter.set(true);
                }

            }
        );
        this.slotCount++;
        this.minHeap.offer(channel);
        this.maxHeap.offer(channel);
    }

    /**
     * Not thread safe implementation of {@link #release(Channel)} that should
     * be gated to single thread access only, followed by memory barrier.
     *
     * @param channel
     */
    private void unsafeRelease(Channel channel)
    {
        if (Optional.ofNullable(channel.attr(CALLOSUM_CLOSING).get()).orElse(false))
        {
            channel.close();
        }
        channel.attr(CALLOSUM_LEASE).set(false);
        this.minHeap.offer(channel);
    }
}
