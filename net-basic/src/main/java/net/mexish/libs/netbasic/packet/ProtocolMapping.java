package net.mexish.libs.netbasic.packet;

public record ProtocolMapping(PacketMapper toServer, PacketMapper toClient) {}
