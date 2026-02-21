package net.mexish.libs.netbasic.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.*;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.netbasic.packet.*;
import net.mexish.libs.netbasic.packet.registry.ProtocolRegistry;
import net.mexish.libs.netbasic.packet.state.ConnectionState;
import net.mexish.libs.netbasic.packet.state.ProtocolState;
import net.mexish.libs.netbasic.packet.dispatcher.DispatcherRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * @author mexish
 */
// TODO proper logging
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Setter
@Getter
public final class PacketHandler extends SimpleChannelInboundHandler<Packet> {

    RequestManager requestManager = new RequestManager();
    DispatcherRegistry registry;
    Object[] logicModules;
    ConnectionListener[] connectionListeners;

    Class<? extends ProtocolState> initialState;

    public PacketHandler(final @NonNull Class<? extends ProtocolState> initialState,
                         final @NonNull DispatcherRegistry registry,
                         final Object @NotNull ... logicModules) {
        this.initialState = initialState;
        this.registry = registry;
        this.logicModules = logicModules;

        val foundListeners = new ArrayList<ConnectionListener>();
        for (val module : logicModules) {
            if (module instanceof ConnectionListener listener) {
                foundListeners.add(listener);
            }
        }

        this.connectionListeners = foundListeners.toArray(ConnectionListener[]::new);
    }

    @Override
    public void channelActive(final @NonNull ChannelHandlerContext ctx) throws Exception {
        setState(initialState, ctx);

        super.channelActive(ctx);

        Thread.ofVirtual().name("conn-active").start(() -> {
            for (val listener : connectionListeners) {
                listener.active(ctx);
            }
        });
    }

    @Override
    public void channelInactive(final @NonNull ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        Thread.ofVirtual().name("conn-inactive").start(() -> {
            for (val listener : connectionListeners) {
                listener.inactive(ctx);
            }
        });
    }

    @Override
    protected void channelRead0(final @NonNull ChannelHandlerContext nettyCtx,
                                final @NonNull Packet packet) {
        var requestId = -1;
        Packet actualPacket;

        if (packet instanceof Packet.Envelope(int id, Packet nigger)) {
            requestId = id;
            actualPacket = nigger;
        } else {
            actualPacket = packet;
        }

        if (requestManager.handleIncoming(packet, requestId)) return;

        val ctx = new PacketProcessorContext(this, nettyCtx, packet, requestId);

        Thread.ofVirtual().name("packet-proc").start(() -> {
            val connectionState = nettyCtx.channel().attr(ConnectionState.KEY).get();

            if (connectionState == null) {
                return;
            }

            val dispatcher = registry.get(connectionState.state());

            if (dispatcher != null) {
                dispatcher.dispatch(logicModules, actualPacket, ctx);
            }
        });
    }

    public void setState(final @NonNull Class<? extends ProtocolState> newState,
                         final @NonNull ChannelHandlerContext ctx) {
        val mapping = ProtocolRegistry.INSTANCE.get(newState);

        if (mapping == null) {
            throw new IllegalStateException("No ProtocolMapping registered for state: " + newState.getName());
        }

        val connectionState = new ConnectionState(newState, mapping);
        ctx.channel().attr(ConnectionState.KEY).set(connectionState);
    }

    public void disconnect(final @NonNull ChannelHandlerContext ctx) {
        for (val listener : connectionListeners) {
            listener.disconnect(ctx);
        }

        ctx.close();
    }
}
