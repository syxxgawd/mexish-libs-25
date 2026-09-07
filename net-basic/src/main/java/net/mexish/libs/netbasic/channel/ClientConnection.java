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
import net.mexish.libs.netbasic.packet.Packet;
import net.mexish.libs.netbasic.packet.RequestManager;
import net.mexish.libs.netbasic.pipeline.PacketHandler;
import net.mexish.libs.netbasic.pipeline.layer.ProtocolLayer;
import net.mexish.libs.netbasic.transport.TransportProfile;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Accessors(fluent = true, chain = true)
@SuppressWarnings("unchecked")
@Setter
public final class ClientConnection {

    TransportProfile profile;
    Map<ChannelOption<?>, Object> options = new LinkedHashMap<>();

    @NonFinal ProtocolLayer protocolLayer;

    @NonFinal RequestManager requestManager;

    @NonFinal Channel channel;

    public ClientConnection(final @NonNull TransportProfile profile) {
        this.profile = profile;
    }

    public <T> ClientConnection option(final @NonNull ChannelOption<T> option,
                                       final @NonNull T value) {
        this.options.put(option, value);
        return this;
    }

    public ChannelFuture connect(final ChannelHandler logicHandler) {
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

                if (logicHandler == null) {
                    return;
                }

                if (logicHandler instanceof PacketHandler handler) {
                    requestManager = handler.getRequestManager();
                }

                ch.pipeline().addLast("logic", logicHandler);
            }
        });

        val future = bootstrap.connect(profile.address());
        channel = future.channel();
        return future;
    }

    public @NotNull ChannelFuture connect() {
        return connect(null);
    }

    public @NotNull CompletableFuture<Packet.Response> createRequest(final @NonNull Packet.Request packet) {
        if (requestManager == null) {
            throw new RuntimeException("unable to create req for packet (requestManager null/logicHandler not instanceof PacketHandler): " + packet.getClass().getSimpleName());
        }

        if (channel == null || !channel.isOpen()) {
            throw new RuntimeException("attempted to create request while client channel is closed");
        }

        return requestManager.createRequest(packet, channel);
    }

    public void sendPacket(final @NonNull Packet packet) {
        if (channel == null || !channel.isOpen()) {
            return;
        }

        channel.writeAndFlush(packet);
    }
}
