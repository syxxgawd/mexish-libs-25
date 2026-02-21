package net.mexish.libs.netbasic.packet.dispatcher;

import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
public interface DispatcherLoader {

    void load(final @NotNull DispatcherRegistry registry);

}
