package com.primeaeterna.callosum.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SlotHandlerTest
{
    private String asString(ByteBuf b, int length)
    {
        return b.readCharSequence(length, StandardCharsets.UTF_8).toString();
    }

    @Test
    public void slotHandlerRespondsWithZeroSlotOnFirstConnection() throws InterruptedException
    {
        final AtomicReference<AssertionError> failure = new AtomicReference<>();
        final SlotHandler slotHandler = new SlotHandler();
        final EventLoopGroup bossGroup = new NioEventLoopGroup();
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        final EventLoopGroup clientGroup = new NioEventLoopGroup();
        try
        {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(LocalServerChannel.class)
             .childHandler(new ChannelInitializer<>() {
                 @Override
                 protected void initChannel(final Channel ch) throws Exception
                 {
                     ch.pipeline().addLast(slotHandler, new ChannelInboundHandlerAdapter()
                     {
                         @Override
                         public void channelActive(final ChannelHandlerContext ctx)
                         {
                             ctx.channel().close();
                             ctx.channel().parent().close();
                         }
                     });
                 }
             });

            ChannelFuture serv = b.bind(new LocalAddress("test")).sync();

            Bootstrap c = new Bootstrap();
            c.group(clientGroup)
             .channel(LocalChannel.class)
             .handler(new ChannelInitializer<>() {
                 @Override
                 protected void initChannel(final Channel ch) throws Exception
                 {
                     ch.pipeline().addLast(new ByteToMessageDecoder() {
                         @Override
                         protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception
                         {
                             if (in.readableBytes() < 3)
                             {
                                 return;
                             }
                             try
                             {
                                 assertTrue(in.isReadable(3));
                                 assertEquals("0\r\n", asString(in, 3));
                             }
                             catch (AssertionError cause)
                             {
                                 failure.set(cause);
                             }
                         }
                     });
                 }
             });
            ChannelFuture client = c.connect(serv.channel().localAddress()).sync();
            client.channel().closeFuture().sync();
            serv.channel().closeFuture().sync();
        }
        finally
        {
            clientGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
        if (failure.get() != null)
        {
            throw failure.get();
        }
    }

    @Test
    public void slotHandlerRespondsWithDifferentSlotsToConcurrentClientsAndAllSlotsAreReturned() throws InterruptedException
    {
        final int numOfClients = 1000;
        final Slots slots = new Slots();
        final SlotHandler slotHandler = new SlotHandler(slots);
        final EventLoopGroup bossGroup = new NioEventLoopGroup();
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        final EventLoopGroup clientGroup = new NioEventLoopGroup();
        try
        {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(LocalServerChannel.class)
             .childHandler(new ChannelInitializer<>() {
                 @Override
                 protected void initChannel(final Channel ch) throws Exception
                 {
                     ch.pipeline().addLast(slotHandler, new ChannelInboundHandlerAdapter()
                     {
                         @Override
                         public void channelActive(final ChannelHandlerContext ctx) {}
                     });
                 }
             });

            ChannelFuture srv = b.bind(new LocalAddress("test")).sync();

            final List<ChannelFuture> clients = new LinkedList<>();
            final Map<String, Boolean> expectedSlots = new ConcurrentHashMap<>();
            final Queue<ChannelHandlerContext> ctxs = new ConcurrentLinkedQueue<>();
            final CountDownLatch latch = new CountDownLatch(numOfClients);
            for (int id = 0; id < numOfClients; id++)
            {
                Bootstrap c = new Bootstrap();
                c.group(clientGroup)
                 .channel(LocalChannel.class)
                 .handler(new ChannelInitializer<>()
                 {
                     @Override
                     protected void initChannel(final Channel ch)
                     {
                         ch.pipeline().addLast(new ChannelInboundHandlerAdapter()
                         {
                             @Override
                             public void channelRead(final ChannelHandlerContext ctx, final Object msg)
                             {
                                 final ByteBuf b = (ByteBuf) msg;
                                 expectedSlots.put(asString(b, b.readableBytes()), true);
                                 ctxs.add(ctx);
                                 latch.countDown();
                             }
                         });
                     }
                 });
                clients.add(c.connect(srv.channel().localAddress()));
            }
            latch.await();
            for (ChannelHandlerContext ctx : ctxs)
            {
                ctx.close();
            }
            for (ChannelFuture client : clients)
            {
                client.channel().closeFuture().sync();
            }
            for (int id = 0; id < numOfClients; id++)
            {
                assertTrue(expectedSlots.containsKey(String.valueOf(id) + "\r\n"), String.valueOf(id));
            }
            Thread.sleep(100); // give all close listeners opportunity to return slots
            for (int slot = 0; slot < numOfClients; slot++)
            {
                assertEquals(slot, slots.next());
            }
        }
        finally
        {
            clientGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
