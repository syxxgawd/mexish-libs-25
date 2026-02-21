package net.mexish.libs.netbasic.pipeline.layer;

import io.netty.channel.ChannelPipeline;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.netbasic.packet.ChannelSide;
import net.mexish.libs.netbasic.pipeline.PacketDecoder;
import net.mexish.libs.netbasic.pipeline.PacketEncoder;
import net.mexish.libs.netbasic.pipeline.VarIntFrameDecoder;
import net.mexish.libs.netbasic.pipeline.VarIntLengthPrepender;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class StandardPacketLayer implements ProtocolLayer {

    ChannelSide side;

    private static final VarIntLengthPrepender LENGTH_PREPENDER = new VarIntLengthPrepender();

    @Override
    public void configure(final @NonNull ChannelPipeline pipe) {
        pipe.addLast(new VarIntFrameDecoder());
        pipe.addLast(new PacketDecoder(side));

        pipe.addLast(LENGTH_PREPENDER);
        pipe.addLast(new PacketEncoder(side));
    }
}
