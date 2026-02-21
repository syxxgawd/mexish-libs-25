package net.mexish.libs.netbasic.transport.impl;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.netbasic.transport.TransportProfile;
import net.mexish.libs.netbasic.util.TransportUtils;

import java.net.InetSocketAddress;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Accessors(fluent = true)
public final class TcpProfile implements TransportProfile {

    @Getter final InetSocketAddress address;
    volatile EventLoopGroup workerGroup;
    volatile EventLoopGroup bossGroup;

    final int nThreads;

    public TcpProfile(final int nThreads,
                      final @NonNull String host,
                      final int port) {
        this.address = new InetSocketAddress(host, port);
        this.nThreads = nThreads;
    }

    @Override
    public Class<? extends Channel> channelClass() {
        return TransportUtils.getSocketChannelClass();
    }

    @Override
    public EventLoopGroup workerGroup() {
        if (workerGroup == null) {
            synchronized (this) {
                if (workerGroup == null) {
                    workerGroup = TransportUtils.newBestEventLoopGroup(nThreads, "netty-tcp-worker-group");
                }
            }
        }

        return workerGroup;
    }

    @Override
    public Class<? extends ServerChannel> serverChannelClass() {
        return TransportUtils.getServerSocketChannelClass();
    }

    @Override
    public EventLoopGroup bossGroup() {
        if (bossGroup == null) {
            synchronized (this) {
                if (bossGroup == null) {
                    bossGroup = TransportUtils.newBestEventLoopGroup(1, "netty-tcp-boss-group");
                }
            }
        }

        return bossGroup;
    }

}
