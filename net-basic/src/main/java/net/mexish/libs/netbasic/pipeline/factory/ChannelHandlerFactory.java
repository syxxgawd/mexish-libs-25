package net.mexish.libs.netbasic.pipeline.factory;

import io.netty.channel.ChannelHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
@FunctionalInterface
public interface ChannelHandlerFactory {

    @NotNull ChannelHandler newHandler();

}
