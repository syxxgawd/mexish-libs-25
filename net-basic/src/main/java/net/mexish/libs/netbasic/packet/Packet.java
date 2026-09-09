package net.mexish.libs.netbasic.packet;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
public interface Packet {

    void write(final @NotNull ByteBuf buf);

    interface Request<R extends Packet.Response> extends Packet {}
    interface Response extends Packet {}

    record Envelope(int requestId, Packet packet) implements Packet {
        @Override
        public void write(ByteBuf buf) {
            packet.write(buf);
        }
    }

}
