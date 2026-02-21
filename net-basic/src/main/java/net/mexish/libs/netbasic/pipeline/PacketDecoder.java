package net.mexish.libs.netbasic.pipeline;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import net.mexish.libs.netbasic.packet.ChannelSide;
import net.mexish.libs.netbasic.packet.Packet;
import net.mexish.libs.netbasic.packet.state.ConnectionState;
import net.mexish.libs.netbasic.util.PacketUtils;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class PacketDecoder extends ByteToMessageDecoder {

    ChannelSide side;

    @Override
    protected void decode(final @NonNull ChannelHandlerContext ctx,
                          final @NonNull ByteBuf in,
                          final @NonNull List<Object> out) {
        val state = ctx.channel().attr(ConnectionState.KEY).get();

        if (state == null) {
            return;
        }

        val mapper = switch (side) {
            case SERVER -> state.mapping().toServer();
            case CLIENT -> state.mapping().toClient();
        };

        if (mapper == null) {
            in.skipBytes(in.readableBytes());
            return;
        }

        in.markReaderIndex();

        try {
            val packetId = PacketUtils.readVarInt(in);
            val packet = mapper.newPacket(packetId, in);

            if (packet == null) {
                in.clear();
                return;
            }

            if (packet instanceof Packet.Request || packet instanceof Packet.Response) {
                out.add(new Packet.Envelope(PacketUtils.readVarInt(in), packet));
            } else {
                out.add(packet);
            }

        } catch (final IndexOutOfBoundsException e) {
            in.resetReaderIndex();
        }
    }
}
