package net.mexish.libs.netbasic.pipeline.layer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.netbasic.packet.ChannelSide;
import net.mexish.libs.netbasic.pipeline.PacketDecoder;
import net.mexish.libs.netbasic.pipeline.PacketEncoder;
import net.mexish.libs.netbasic.pipeline.VarIntFrameDecoder;
import net.mexish.libs.netbasic.pipeline.VarIntLengthPrepender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * temporary ? idk it is good tho
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@RequiredArgsConstructor
public class WebSocketLayer implements ProtocolLayer {

    ChannelSide side;
    String wsPath;
    WebSocketClientHandshaker clientHandshaker;

    @Contract("_ -> new")
    public static @NotNull WebSocketLayer server(final String wsPath) {
        return new WebSocketLayer(ChannelSide.SERVER, wsPath == null ? "/ws" : wsPath, null);
    }

    @Contract(" -> new")
    public static @NotNull WebSocketLayer server() {
        return server("/ws");
    }

    @Contract("_ -> new")
    public static @NotNull WebSocketLayer client(final @NonNull WebSocketClientHandshaker handshaker) {
        return new WebSocketLayer(ChannelSide.CLIENT, null, handshaker);
    }

    public static final String WS_HANDLER = "ws_protocol";

    public static final String FRAME_ENCODER = "frame_encoder";
    public static final String FRAME_DECODER = "frame_decoder";

    public static final class PacketToFrameEncoder extends MessageToMessageEncoder<ByteBuf> {
        @Override
        protected void encode(final @NonNull ChannelHandlerContext ctx,
                              final @NonNull ByteBuf msg,
                              final @NonNull List<Object> out) {
            out.add(new BinaryWebSocketFrame(msg.retain()));
        }
    }

    public static final class FrameToPacketDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {
        @Override
        protected void decode(final @NonNull ChannelHandlerContext ctx,
                              final @NonNull BinaryWebSocketFrame msg,
                              final @NonNull List<Object> out) {
            out.add(msg.content().retain());
        }
    }

    @Override
    public void configure(final @NotNull ChannelPipeline pipe) {
        switch (side) {
            case CLIENT -> {
                pipe.addLast(new HttpClientCodec());
                pipe.addLast(new HttpObjectAggregator(65536));
                pipe.addLast(WS_HANDLER, new WebSocketClientProtocolHandler(clientHandshaker));
            }

            case SERVER -> {
                pipe.addLast(new HttpServerCodec());
                pipe.addLast(new HttpObjectAggregator(65536));
                pipe.addLast(WS_HANDLER, new WebSocketServerProtocolHandler(wsPath));
            }
        }

        pipe.addLast(FRAME_DECODER, new FrameToPacketDecoder());
        pipe.addLast(StandardPacketLayer.PACKET_DECODER, new PacketDecoder(side));
        pipe.addLast(FRAME_ENCODER, new PacketToFrameEncoder());
        pipe.addLast(StandardPacketLayer.PACKET_ENCODER, new PacketEncoder(side));
    }
}
