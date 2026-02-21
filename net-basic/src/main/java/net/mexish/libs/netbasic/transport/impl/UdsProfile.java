package net.mexish.libs.netbasic.transport.impl;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.unix.DomainSocketAddress;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.netbasic.transport.TransportProfile;
import net.mexish.libs.netbasic.util.TransportUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Accessors(fluent = true)
public final class UdsProfile implements TransportProfile {

    @Getter final DomainSocketAddress address;
    volatile EventLoopGroup workerGroup;
    volatile EventLoopGroup bossGroup;

    final int nThreads;

    public UdsProfile(final int nThreads,
                      final @NotNull String path) {
        this.address = new DomainSocketAddress(path);
        this.nThreads = nThreads;
    }

    @Override
    public Class<? extends Channel> channelClass() {
        return TransportUtils.getDomainSocketChannelClass();
    }

    @Override
    public EventLoopGroup workerGroup() {
        if (workerGroup == null) {
            synchronized (this) {
                if (workerGroup == null) {
                    workerGroup = TransportUtils.newBestEventLoopGroup(nThreads, "netty-uds-worker-group");
                }
            }
        }

        return workerGroup;
    }

    @Override
    public Class<? extends ServerChannel> serverChannelClass() {
        return TransportUtils.getServerDomainSocketChannelClass();
    }

    @Override
    public EventLoopGroup bossGroup() {
        if (bossGroup == null) {
            synchronized (this) {
                if (bossGroup == null) {
                    bossGroup = TransportUtils.newBestEventLoopGroup(1, "netty-uds-boss-group");
                }
            }
        }

        return bossGroup;
    }

}
