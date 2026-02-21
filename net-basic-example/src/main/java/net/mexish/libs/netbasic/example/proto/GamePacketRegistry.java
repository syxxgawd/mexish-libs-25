package net.mexish.libs.netbasic.example.proto;

import lombok.val;
import net.mexish.libs.netbasic.packet.PacketMapper;
import net.mexish.libs.netbasic.packet.ProtocolBuilder;
import net.mexish.libs.netbasic.packet.ProtocolMapping;
import net.mexish.libs.netbasic.example.proto.Packets.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class GamePacketRegistry {

    public static final ProtocolMapping HANDSHAKE = build(b -> b
            .server(0x00, HandshakeRequest.class, HandshakeRequest::new)
            .client(0x00, HandshakeResponse.class, HandshakeResponse::new)
    );

//    public static final ProtocolMapping LOGIN = build(b -> b
//            .server(0x00, LoginRequest.class)
//            .client(0x00, LoginResponse.class)
//            .shared(0x01, KeepAlivePacket.class)
//    );
//
//    public static final ProtocolMapping PLAY = build(b -> b
//            .server(0x00, ChatMessageToServer.class)
//            .client(0x01, ChatMessageToClient.class)
//            .shared(0x02, KeepAlivePacket.class)
//    );

    @Contract("_ -> new")
    private static @NotNull ProtocolMapping build(final @NotNull Consumer<ProtocolBuilder> consumer) {
        val toServer = new PacketMapper();
        val toClient = new PacketMapper();

        consumer.accept(new ProtocolBuilder(toServer, toClient));
        return new ProtocolMapping(toServer, toClient);
    }
}
