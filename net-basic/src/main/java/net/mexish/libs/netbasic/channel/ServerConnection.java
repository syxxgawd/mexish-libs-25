package net.mexish.libs.netbasic.channel;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;
import net.mexish.libs.netbasic.pipeline.layer.ProtocolLayer;
import net.mexish.libs.netbasic.pipeline.factory.ChannelHandlerFactory;
import net.mexish.libs.netbasic.transport.TransportProfile;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Accessors(fluent = true, chain = true)
@SuppressWarnings("unchecked")
@Setter
public final class ServerConnection {

    TransportProfile profile;
    Map<ChannelOption<?>, Object> childOptions = new ConcurrentHashMap<>();

    @NonFinal ProtocolLayer protocolLayer;

    public ServerConnection(final @NonNull TransportProfile profile) {
        this.profile = profile;
    }

    public <T> ServerConnection childOption(final @NonNull ChannelOption<T> option,
                                            final @NonNull T value) {
        this.childOptions.put(option, value);
        return this;
    }

    public ChannelFuture bind(final @Nullable ChannelHandlerFactory logicFactory) {
        val bootstrap = new ServerBootstrap();

        bootstrap.group(profile.bossGroup(), profile.workerGroup())
                .channel(profile.serverChannelClass());

        childOptions.forEach((key, value) -> bootstrap.childOption((ChannelOption<Object>) key, value));

        bootstrap.childHandler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(final @NonNull Channel ch) {
                if (protocolLayer != null) {
                    protocolLayer.configure(ch.pipeline());
                }

                if (logicFactory != null) {
                    ch.pipeline().addLast("logic", logicFactory.newHandler());
                }
            }
        });

        return bootstrap.bind(profile.address());
    }

    public ChannelFuture bind() {
        return bind(null);
    }

}
