package net.mexish.libs.netbasic.transport;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

import java.net.SocketAddress;

/**
 * @author mexish
 */
public interface TransportProfile {

    Class<? extends Channel> channelClass();

    SocketAddress address();

    EventLoopGroup workerGroup();

    Class<? extends ServerChannel> serverChannelClass();

    EventLoopGroup bossGroup();

}
