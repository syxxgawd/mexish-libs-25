package net.mexish.libs.netbasic.packet.dispatcher;

import lombok.NonNull;
import net.mexish.libs.netbasic.packet.Packet;
import net.mexish.libs.netbasic.packet.PacketProcessorContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
public interface PacketDispatcher {

    void dispatch(final @NonNull Object[] logicModules,
                  final @NotNull Packet packet,
                  final @NotNull PacketProcessorContext ctx);

}
