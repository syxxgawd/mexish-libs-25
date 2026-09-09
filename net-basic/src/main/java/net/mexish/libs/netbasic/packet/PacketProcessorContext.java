package net.mexish.libs.netbasic.packet;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import lombok.NonNull;
import net.mexish.libs.netbasic.packet.state.ProtocolState;
import net.mexish.libs.netbasic.pipeline.PacketHandler;

/**
 * @author mexish
 * @param nettyCtx the original netty context
 * @param packet the packet that is currently being processed
 */
public record PacketProcessorContext(PacketHandler handler, ChannelHandlerContext nettyCtx, Packet packet, int requestId) {

    public ChannelFuture send(final @NonNull Packet packet) {
        if (packet instanceof Packet.Response && requestId != -1) {
            return nettyCtx.writeAndFlush(new Packet.Envelope(requestId, packet));
        }

        return nettyCtx.writeAndFlush(packet);
    }

    public void switchState(final @NonNull Class<? extends ProtocolState> newState) {
        handler.setState(newState, nettyCtx);
    }

    public void disconnect() {
        handler.disconnect(nettyCtx);
    }

}
