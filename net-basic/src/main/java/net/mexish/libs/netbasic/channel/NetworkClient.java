package net.mexish.libs.netbasic.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;
import net.mexish.libs.netbasic.pipeline.layer.ProtocolLayer;
import net.mexish.libs.netbasic.transport.TransportProfile;

import java.util.LinkedHashMap;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Accessors(fluent = true, chain = true)
@SuppressWarnings("unchecked")
@Setter
public final class NetworkClient {

    TransportProfile profile;
    Map<ChannelOption<?>, Object> options = new LinkedHashMap<>();

    @NonFinal ProtocolLayer protocolLayer;

    public NetworkClient(final @NonNull TransportProfile profile) {
        this.profile = profile;
    }

    public <T> NetworkClient option(final @NonNull ChannelOption<T> option,
                                    final @NonNull T value) {
        this.options.put(option, value);
        return this;
    }

    public ChannelFuture connect(@NonNull ChannelHandler logicHandler) {
        val bootstrap = new Bootstrap();

        bootstrap.group(profile.workerGroup())
                .channel(profile.channelClass());

        options.forEach((key, value) -> bootstrap.option((ChannelOption<Object>) key, value));

        bootstrap.handler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(final @NonNull Channel ch) {
                if (protocolLayer != null) {
                    protocolLayer.configure(ch.pipeline());
                }

                ch.pipeline().addLast("logic", logicHandler);
            }
        });

        return bootstrap.connect(profile.address());
    }
}
