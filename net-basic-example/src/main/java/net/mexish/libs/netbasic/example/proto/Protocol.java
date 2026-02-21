package net.mexish.libs.netbasic.example.proto;

import net.mexish.libs.netbasic.packet.state.ProtocolState;

public final class Protocol {
    private Protocol() {}

    public static final class Handshake implements ProtocolState {}
    public static final class Login implements ProtocolState {}
    public static final class Play implements ProtocolState {}
}
