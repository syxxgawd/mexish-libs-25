package net.mexish.libs.netbasic.packet;

import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
public interface ConnectionListener {

    default void disconnect(final @NotNull ChannelHandlerContext ctx) {
        // noop
    }

    default void active(final @NotNull ChannelHandlerContext ctx) {
        // noop
    }

    default void inactive(final @NotNull ChannelHandlerContext ctx) {
        // noop
    }

}
