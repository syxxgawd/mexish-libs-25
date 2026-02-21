package net.mexish.libs.netbasic.packet;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class ProtocolBuilder {

    PacketMapper TO_SERVER;
    PacketMapper TO_CLIENT;

    public <T extends Packet> ProtocolBuilder server(int id,
                                                     Class<T> packetClass,
                                                     final @NonNull PacketFactory factory) {
        TO_SERVER.registerPacket(id, packetClass, factory);
        return this;
    }

    public <T extends Packet> ProtocolBuilder client(int id,
                                                     Class<T> packetClass,
                                                     final @NonNull PacketFactory factory) {
        TO_CLIENT.registerPacket(id, packetClass, factory);
        return this;
    }

    public <T extends Packet> ProtocolBuilder shared(int id,
                                                     Class<T> packetClass,
                                                     final @NonNull PacketFactory factory) {
        server(id, packetClass, factory);
        client(id, packetClass, factory);
        return this;
    }
}
