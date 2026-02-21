package net.mexish.libs.netbasic.util;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.*;
import io.netty.channel.kqueue.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.uring.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
@UtilityClass
public final class TransportUtils {

    public static final TransportType BEST_TRANSPORT = detectBestTransport();

    public enum TransportType {
        IO_URING,
        EPOLL,
        KQUEUE,
        NIO
    }

    private static TransportType detectBestTransport() {
        if (IoUring.isAvailable()) {
            return TransportType.IO_URING;
        }

        if (Epoll.isAvailable()) {
            return TransportType.EPOLL;
        }

        if (KQueue.isAvailable()) {
            return TransportType.KQUEUE;
        }

        return TransportType.NIO;
    }

    public @NotNull EventLoopGroup newBestEventLoopGroup(final int threads,
                                                         final @NonNull String threadNamePrefix) {
        return newEventLoopGroup(BEST_TRANSPORT, threads, threadNamePrefix);
    }

    public @NotNull EventLoopGroup newEventLoopGroup(final @NonNull TransportType type,
                                                     final int threads,
                                                     final @NonNull String threadNamePrefix) {
        val factory = new DefaultThreadFactory(threadNamePrefix, true);

        return switch (type) {
            case IO_URING -> new MultiThreadIoEventLoopGroup(threads, factory, IoUringIoHandler.newFactory());
            case EPOLL    -> new MultiThreadIoEventLoopGroup(threads, factory, EpollIoHandler.newFactory());
            case KQUEUE   -> new MultiThreadIoEventLoopGroup(threads, factory, KQueueIoHandler.newFactory());
            case NIO      -> new MultiThreadIoEventLoopGroup(threads, factory, NioIoHandler.newFactory());
        };
    }

    public Class<? extends Channel> getSocketChannelClass() {
        return switch (BEST_TRANSPORT) {
            case IO_URING -> IoUringSocketChannel.class;
            case EPOLL    -> EpollSocketChannel.class;
            case KQUEUE   -> KQueueSocketChannel.class;
            case NIO      -> NioSocketChannel.class;
        };
    }

    public Class<? extends ServerChannel> getServerSocketChannelClass() {
        return switch (BEST_TRANSPORT) {
            case IO_URING -> IoUringServerSocketChannel.class;
            case EPOLL    -> EpollServerSocketChannel.class;
            case KQUEUE   -> KQueueServerSocketChannel.class;
            case NIO      -> NioServerSocketChannel.class;
        };
    }

    public Class<? extends Channel> getDomainSocketChannelClass() {
        return switch (BEST_TRANSPORT) {
            case EPOLL    -> EpollDomainSocketChannel.class;
            case KQUEUE   -> KQueueDomainSocketChannel.class;
            case IO_URING -> IoUringDomainSocketChannel.class;
            case NIO      -> throw new UnsupportedOperationException("Unix Domain Sockets are not supported on standard NIO (Windows/Unsupported OS). Use Epoll or KQueue.");
        };
    }

    public Class<? extends ServerChannel> getServerDomainSocketChannelClass() {
        return switch (BEST_TRANSPORT) {
            case EPOLL    -> EpollServerDomainSocketChannel.class;
            case KQUEUE   -> KQueueServerDomainSocketChannel.class;
            case IO_URING -> IoUringServerDomainSocketChannel.class;
            case NIO      -> throw new UnsupportedOperationException("Unix Domain Sockets are not supported on standard NIO.");
        };
    }

}
