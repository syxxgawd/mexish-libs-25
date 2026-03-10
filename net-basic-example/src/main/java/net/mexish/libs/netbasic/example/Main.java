package net.mexish.libs.netbasic.example;

import net.mexish.libs.netbasic.channel.ServerConnection;
import net.mexish.libs.netbasic.example.logic.ChatLogic;
import net.mexish.libs.netbasic.example.proto.GamePacketRegistry;
import net.mexish.libs.netbasic.example.proto.Protocol;
import net.mexish.libs.netbasic.packet.ChannelSide;
import net.mexish.libs.netbasic.packet.dispatcher.DispatcherRegistry;
import net.mexish.libs.netbasic.packet.registry.ProtocolRegistry;
import net.mexish.libs.netbasic.pipeline.PacketHandler;
import net.mexish.libs.netbasic.pipeline.layer.StandardPacketLayer;
import net.mexish.libs.netbasic.transport.shared.SharedTcpProfile;

public class Main {

    void main() {
        IO.println("Starting Chat Server...");

        ProtocolRegistry.INSTANCE.register(Protocol.Handshake.class, GamePacketRegistry.HANDSHAKE);
//        ProtocolRegistry.INSTANCE.register(Protocol.Login.class, GamePacketRegistry.LOGIN);
//        ProtocolRegistry.INSTANCE.register(Protocol.Play.class, GamePacketRegistry.PLAY);

        DispatcherRegistry dispatcherRegistry = new DispatcherRegistry();
        dispatcherRegistry.loadAutomatically();

        final Object[] logicModules = {
                new ChatLogic()
        };

        try {
            var future = new ServerConnection(new SharedTcpProfile("0.0.0.0", 8080))
                    .protocolLayer(new StandardPacketLayer(ChannelSide.SERVER))
                    .bind(() -> new PacketHandler(
                            Protocol.Handshake.class,
                            dispatcherRegistry,
                            logicModules
                    ))
                    .syncUninterruptibly();

            if (future.isSuccess()) {
                IO.println("Server started successfully on port 8080.");
                future.channel().closeFuture().syncUninterruptibly();
            } else {
                IO.println("Failed to start server.");
                future.cause().printStackTrace();
            }
        } catch (Exception e) {
            IO.println("An error occurred during startup:");
            e.printStackTrace();
        } finally {
            IO.println("Server shut down.");
        }
    }
}
