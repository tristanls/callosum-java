package com.primeaeterna.callosum.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * The slot handler is responsible for responding to any TCP connection by
 * providing a slot number followed by {@code \r\n}
 */
public class SlotHandler extends ChannelInboundHandlerAdapter
{
    private static byte[] CRLF = new byte[] { '\r', '\n' };
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
        final ByteBuf msg = ctx.alloc().buffer(6); // int + \r\n
        msg.writeInt(slot);
        msg.writeBytes(CRLF);

        ctx.channel().closeFuture().addListener((future) -> this.slots.put(slot));

        ctx.writeAndFlush(msg);
        ctx.fireChannelActive();
    }
}
