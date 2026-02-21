package net.mexish.libs.netbasic.packet.state;

import io.netty.util.AttributeKey;
import net.mexish.libs.netbasic.packet.ProtocolMapping;

/**
 * @author mexish
 */
public record ConnectionState(Class<? extends ProtocolState> state, ProtocolMapping mapping) {
    public static final AttributeKey<ConnectionState> KEY = AttributeKey.valueOf("connectionState");
}
