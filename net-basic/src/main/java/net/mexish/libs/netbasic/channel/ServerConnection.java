package net.mexish.libs.netbasic.channel;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;
import net.mexish.libs.netbasic.packet.Packet;
import net.mexish.libs.netbasic.packet.RequestManager;
import net.mexish.libs.netbasic.packet.registry.ProtocolRegistry;
import net.mexish.libs.netbasic.packet.state.ConnectionState;
import net.mexish.libs.netbasic.packet.state.ProtocolState;
import net.mexish.libs.netbasic.pipeline.PacketHandler;
import net.mexish.libs.netbasic.pipeline.layer.ProtocolLayer;
import net.mexish.libs.netbasic.pipeline.factory.ChannelHandlerFactory;
import net.mexish.libs.netbasic.transport.TransportProfile;
import net.mexish.libs.netbasic.transport.impl.UdsProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Accessors(fluent = true, chain = true)
@SuppressWarnings("unchecked")
public final class ServerConnection {

    TransportProfile profile;
    Map<ChannelOption<?>, Object> childOptions = new ConcurrentHashMap<>();

    // think about finalizing it perhaps
    @Setter @NonFinal ProtocolLayer protocolLayer;

    public ServerConnection(final @NonNull TransportProfile profile) {
        this.profile = profile;

        if (profile instanceof UdsProfile udsProfile) {
            try {
                Files.deleteIfExists(Paths.get(udsProfile.address().path()));
            } catch (final Throwable ignored) {
            }
        }
    }

    public <T> ServerConnection childOption(final @NonNull ChannelOption<T> option,
                                            final @NonNull T value) {
        this.childOptions.put(option, value);
        return this;
    }

    public ChannelFuture bind(final @Nullable ChannelHandlerFactory logicFactory) {
        val bootstrap = new ServerBootstrap()
                .group(profile.bossGroup(), profile.workerGroup())
                .channel(profile.serverChannelClass())
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(final @NonNull Channel ch) {
                        if (protocolLayer != null) {
                            protocolLayer.configure(ch.pipeline());
                        }

                        if (logicFactory != null) {
                            val logicHandler = logicFactory.newHandler();
                            ch.pipeline().addLast("logic", logicHandler);

                            if (logicHandler instanceof PacketHandler handler) {
                                upgradeConnection(ch, handler.getInitialState());
                            }
                        }
                    }
        });

        childOptions.forEach((key, value) -> bootstrap.childOption((ChannelOption<Object>) key, value));
        return bootstrap.bind(profile.address()).addListener(bs -> {
            if (!bs.isSuccess()) {
                return;
            }

            if (profile instanceof UdsProfile _profile) { // include for shared/think of a cleaner way
                Files.setPosixFilePermissions(Paths.get(_profile.address().path()), PosixFilePermissions.fromString("rwxrwxrwx"));
            }
        });
    }

    public void upgradeConnection(final @NonNull Channel channel,
                                  final @NonNull Class<? extends ProtocolState> newState) {
        val mapping = ProtocolRegistry.INSTANCE.get(newState);

        if (mapping == null) {
            throw new IllegalStateException("No ProtocolMapping registered for state: " + newState.getName());
        }

        channel.attr(ConnectionState.KEY).set(new ConnectionState(newState, mapping));
    }

    public ChannelFuture bind() {
        return bind(null);
    }

}
