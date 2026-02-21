package net.mexish.libs.netbasic.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.NonNull;
import net.mexish.libs.netbasic.util.PacketUtils;

import java.util.List;

/**
 * @author mexish
 */
public final class VarIntFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(final @NonNull ChannelHandlerContext ctx,
                          final @NonNull ByteBuf in,
                          final @NonNull List<Object> out) {
        if (!in.isReadable()) {
            return;
        }

        in.markReaderIndex();

        final int length;
        try {
            length = PacketUtils.readVarInt(in);
        } catch (final IndexOutOfBoundsException e) {
            in.resetReaderIndex();
            return;
        }

        if (length < 0) {
            throw new CorruptedFrameException("Received negative length frame: " + length);
        }

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        out.add(in.readRetainedSlice(length));
    }
}
