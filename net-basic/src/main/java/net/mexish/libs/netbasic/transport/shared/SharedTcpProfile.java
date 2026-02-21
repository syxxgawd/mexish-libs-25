package net.mexish.libs.netbasic.transport.shared;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.netbasic.share.NettyResources;
import net.mexish.libs.netbasic.transport.TransportProfile;
import net.mexish.libs.netbasic.util.TransportUtils;

import java.net.InetSocketAddress;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Accessors(fluent = true)
public final class SharedTcpProfile implements TransportProfile {

    @Getter InetSocketAddress address;

    public SharedTcpProfile(final @NonNull String host,
                            final int port) {
        this.address = new InetSocketAddress(host, port);
    }

    @Override
    public Class<? extends Channel> channelClass() {
        return TransportUtils.getSocketChannelClass();
    }

    @Override
    public EventLoopGroup workerGroup() {
        return NettyResources.INSTANCE.getSharedWorker(TransportUtils.BEST_TRANSPORT);
    }

    @Override
    public Class<? extends ServerChannel> serverChannelClass() {
        return TransportUtils.getServerSocketChannelClass();
    }

    @Override
    public EventLoopGroup bossGroup() {
        return NettyResources.INSTANCE.getSharedBoss(TransportUtils.BEST_TRANSPORT);
    }

}
