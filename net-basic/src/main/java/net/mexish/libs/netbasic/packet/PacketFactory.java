package net.mexish.libs.netbasic.packet;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
public interface PacketFactory {

    @NotNull Packet create(final @NotNull ByteBuf buf);

}
