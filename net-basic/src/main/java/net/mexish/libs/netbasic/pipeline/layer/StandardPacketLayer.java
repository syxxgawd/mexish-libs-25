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
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@RequiredArgsConstructor
public class StandardPacketLayer implements ProtocolLayer {

    ChannelSide side;

    private static final VarIntLengthPrepender LENGTH_PREPENDER = new VarIntLengthPrepender();

    public static final String FRAME_DECODER = "frame_decoder";
    public static final String PACKET_DECODER = "packet_decoder";
    public static final String PREPENDER = "length_prepender";
    public static final String PACKET_ENCODER = "packet_encoder";

    @Override
    public void configure(final @NonNull ChannelPipeline pipe) {
        pipe.addLast(FRAME_DECODER, new VarIntFrameDecoder());
        pipe.addLast(PACKET_DECODER, new PacketDecoder(side));
        pipe.addLast(PREPENDER, LENGTH_PREPENDER);
        pipe.addLast(PACKET_ENCODER, new PacketEncoder(side));
    }
}
