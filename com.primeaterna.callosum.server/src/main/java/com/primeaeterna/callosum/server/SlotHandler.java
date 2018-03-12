package com.primeaeterna.callosum.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * The slot handler is responsible for responding to any TCP connection by
 * providing a slot number followed by {@code \r\n}
 */
@ChannelHandler.Sharable
public class SlotHandler extends ChannelInboundHandlerAdapter
{

    private static byte[] CRLF = "\r\n".getBytes();
    private Slots slots;

    /**
     * Creates a new {@link SlotHandler} with its own slot tracking.
     */
    public SlotHandler()
    {
        super();
        this.slots = new Slots();
    }

    /**
     * For every connection, respond with slot number and {@code \r\n}, then
     * ensure slot is returned once connection closes in any way.
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx)
    {
        final int slot = this.slots.next();
        byte[] slotBytes = String.valueOf(slot).getBytes();
        final ByteBuf msg = ctx.alloc().buffer(slotBytes.length + 2); // int + \r\n
        msg.writeBytes(slotBytes);
        msg.writeBytes(CRLF);

        ctx.channel().closeFuture().addListener((future) -> this.slots.put(slot));

        ctx.writeAndFlush(msg);
        ctx.fireChannelActive();
    }
}
