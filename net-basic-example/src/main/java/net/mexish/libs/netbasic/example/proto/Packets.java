package net.mexish.libs.netbasic.example.proto;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.mexish.libs.netbasic.annotation.PacketState;
import net.mexish.libs.netbasic.packet.Packet;
import net.mexish.libs.netbasic.util.PacketUtils;
import org.jetbrains.annotations.NotNull;

public final class Packets {
    private Packets() {}

    @PacketState(Protocol.Handshake.class)
    public record HandshakeRequest(int version) implements Packet.Request {

        public HandshakeRequest(final ByteBuf buf) {
            this(PacketUtils.readVarInt(buf));
        }

        @Override
        public void write(final @NonNull ByteBuf buf) {
            PacketUtils.writeVarInt(buf, version);
        }
    }

    @PacketState(Protocol.Handshake.class)
    public record HandshakeResponse(boolean success) implements Packet.Response {
        public HandshakeResponse(final ByteBuf buf) {
            this(buf.readBoolean());
        }

        @Override
        public void write(@NotNull ByteBuf buf) {
            buf.writeBoolean(success);
        }
    }

    @PacketState(Protocol.Login.class)
    public record LoginRequest(int id, String username) implements Packet.Request {
        @Override public void write(@NotNull ByteBuf buf) { PacketUtils.writeString(buf, username); }
    }

    @PacketState(Protocol.Login.class)
    public record LoginResponse(int id, boolean success, String message) implements Packet.Response {
        @Override public void write(@NotNull ByteBuf buf) { buf.writeBoolean(success); PacketUtils.writeString(buf, message); }
    }

    @PacketState(Protocol.Play.class)
    public record ChatMessageToServer(String message) implements Packet {
        @Override public void write(@NotNull ByteBuf buf) { PacketUtils.writeString(buf, message); }
    }

    @PacketState(Protocol.Play.class)
    public record ChatMessageToClient(String sender, String message) implements Packet {
        @Override public void write(@NotNull ByteBuf buf) { PacketUtils.writeString(buf, sender); PacketUtils.writeString(buf, message); }
    }

    @PacketState({Protocol.Login.class, Protocol.Play.class})
    public record KeepAlivePacket() implements Packet {
        @Override public void write(@NotNull ByteBuf buf) {}
    }
}
