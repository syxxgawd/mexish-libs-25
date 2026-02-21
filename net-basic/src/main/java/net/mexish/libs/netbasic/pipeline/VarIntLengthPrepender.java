package net.mexish.libs.netbasic.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.NonNull;
import lombok.val;
import net.mexish.libs.netbasic.util.PacketUtils;

/**
 * @author mexish
 */
@ChannelHandler.Sharable
public final class VarIntLengthPrepender extends MessageToByteEncoder<ByteBuf> {

    @Override
    protected void encode(final @NonNull ChannelHandlerContext ctx,
                          final @NonNull ByteBuf msg,
                          final @NonNull ByteBuf out) {
        val bodyLength = msg.readableBytes();
        val headerLength = PacketUtils.getVarIntSize(bodyLength);
        out.ensureWritable(headerLength + bodyLength);
        PacketUtils.writeVarInt(out, bodyLength);
        out.writeBytes(msg);
    }

}
