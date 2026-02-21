package net.mexish.libs.netbasic.pipeline.layer;

import io.netty.channel.ChannelPipeline;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
@FunctionalInterface
public interface ProtocolLayer {

    void configure(final @NotNull ChannelPipeline pipe);

}
