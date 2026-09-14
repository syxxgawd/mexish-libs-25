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
import net.mexish.libs.netbasic.packet.ProtocolMapping;
import net.mexish.libs.netbasic.packet.RequestManager;
import net.mexish.libs.netbasic.packet.registry.ProtocolRegistry;
import net.mexish.libs.netbasic.packet.state.ConnectionState;
import net.mexish.libs.netbasic.packet.state.ProtocolState;
import net.mexish.libs.netbasic.pipeline.PacketHandler;
import net.mexish.libs.netbasic.pipeline.layer.ProtocolLayer;
import net.mexish.libs.netbasic.transport.TransportProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NonFinal ChannelHandler logicHandler;

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

    public @NotNull ChannelFuture connect(final ChannelHandler logicHandler) {
        this.logicHandler = logicHandler;

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
                    upgradeConnection(ch, handler.getInitialState());
                }

                ch.pipeline().addLast("logic", logicHandler);
            }
        });

        val future = bootstrap.connect(profile.address());
        channel = future.channel();
        return future;
    }

    public @NotNull ChannelFuture connect() {
        return connect(logicHandler);
    }

    public <R extends Packet.Response> @NotNull CompletableFuture<R> createRequest(final @NonNull Packet.Request<R> packet) {
        if (requestManager == null) {
            throw new RuntimeException("unable to create req for packet (requestManager null/logicHandler not instanceof PacketHandler): " + packet.getClass().getSimpleName());
        }

        if (channel == null || !channel.isActive()) {
            throw new RuntimeException("attempted to create request while client channel is closed");
        }

        return requestManager.createRequest(packet, channel);
    }

    public ChannelFuture sendPacket(final @NonNull Packet packet) {
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("attempted sending a packet while disconnected");
        }

        return channel.writeAndFlush(packet);
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public ChannelFuture disconnect() {
        if (channel == null) {
            throw new IllegalStateException("attempted disconnecting without ever initializing a channel!");
        }

        return channel.close();
    }

    public void upgradeConnection(final @NonNull Class<? extends ProtocolState> newState) {
        if (channel == null) {
            throw new IllegalStateException("attempted upgrading connection without ever initializing a channel!");
        }

        upgradeConnection(channel, newState);
    }

    public void upgradeConnection(final @NonNull Channel channel,
                                  final @NonNull Class<? extends ProtocolState> newState) {
        val mapping = ProtocolRegistry.INSTANCE.get(newState);

        if (mapping == null) {
            throw new IllegalStateException("No ProtocolMapping registered for state: " + newState.getName());
        }

        channel.attr(ConnectionState.KEY).set(new ConnectionState(newState, mapping));
    }

    public @NotNull Class<? extends ProtocolState> getState() {
        if (channel == null) {
            throw new IllegalStateException("cannot retrieve state while channel isn't initialized");
        }

        return channel.attr(ConnectionState.KEY).get().state();
    }

}
