package net.mexish.libs.netbasic.annotation;

import net.mexish.libs.netbasic.packet.state.ProtocolState;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface PacketState {

    Class<? extends ProtocolState>[] value();

}
