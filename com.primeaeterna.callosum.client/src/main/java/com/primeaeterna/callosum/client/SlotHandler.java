package com.primeaeterna.callosum.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * The slot handler responsible for detecting TCP connection slot numbers, which
 * are the first data on the connection in form of slot number followed by
 * {@code \r\n}
 */
@ChannelHandler.Sharable
public class SlotHandler extends ChannelInboundHandlerAdapter
{
    public SlotHandler()
    {
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception
    {
        super.channelRead(ctx, msg);
    }
}
