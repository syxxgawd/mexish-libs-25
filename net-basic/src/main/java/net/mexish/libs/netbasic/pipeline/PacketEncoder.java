package net.mexish.libs.netbasic.pipeline;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import net.mexish.libs.netbasic.packet.ChannelSide;
import net.mexish.libs.netbasic.packet.Packet;
import net.mexish.libs.netbasic.packet.PacketMapper;
import net.mexish.libs.netbasic.packet.state.ConnectionState;
import net.mexish.libs.netbasic.util.PacketUtils;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class PacketEncoder extends MessageToByteEncoder<Packet> {

    ChannelSide side;

    @Override
    protected void encode(final @NonNull ChannelHandlerContext ctx,
                          final @NonNull Packet packet,
                          final @NonNull ByteBuf out) {
        var requestId = -1;
        var actualPacket = packet;

        if (packet instanceof Packet.Envelope(int id, Packet nigger)) {
            requestId = id;
            actualPacket = nigger;
        }

        val state = ctx.channel().attr(ConnectionState.KEY).get();

        if (state == null) {
            return;
        }

        val mapper = switch (side) {
            case SERVER -> state.mapping().toClient();
            case CLIENT -> state.mapping().toServer();
        };

        if (mapper == null) {
            return;
        }

        val packetId = mapper.getPacketId(actualPacket.getClass());

        if (packetId == -1) {
            throw new IllegalStateException("Unregistered packet sent: " + actualPacket.getClass().getSimpleName() + " in state " + state.state().getSimpleName());
        }

        PacketUtils.writeVarInt(out, packetId);
        packet.write(out);

        if (requestId != -1
                || actualPacket instanceof Packet.Request
                || actualPacket instanceof Packet.Response) {
            PacketUtils.writeVarInt(out, requestId);
        }
    }
}
