package net.mexish.libs.netbasic.example.logic;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.val;
import net.mexish.libs.netbasic.annotation.PacketHandler;
import net.mexish.libs.netbasic.example.proto.Packets;
import net.mexish.libs.netbasic.example.proto.Protocol;
import net.mexish.libs.netbasic.packet.ConnectionListener;
import net.mexish.libs.netbasic.packet.PacketProcessorContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatLogic implements ConnectionListener {

    private static final int PROTOCOL_VERSION = 1;
    private final Map<Channel, String> loggedInUsers = new ConcurrentHashMap<>();

    @Override
    public void active(ChannelHandlerContext ctx) {
        System.out.println("New connection from: " + ctx.channel().remoteAddress());
    }

    @Override
    public void inactive(ChannelHandlerContext ctx) {
        val username = loggedInUsers.remove(ctx.channel());

        if (username != null) {
            IO.println("User '" + username + "' disconnected.");
            broadcast(new Packets.ChatMessageToClient("Server", username + " has left the chat."));
        } else {
            IO.println("A client disconnected before logging in.");
        }
    }

    @PacketHandler
    public void onHandshake(Packets.HandshakeRequest packet, PacketProcessorContext ctx) {
        if (packet.version() == PROTOCOL_VERSION) {
            IO.println("Handshake successful. Switching to LOGIN state.");
            ctx.send(new Packets.HandshakeResponse(true));
            ctx.switchState(Protocol.Login.class);
        } else {
            IO.println("Handshake failed due to wrong version.");
            ctx.send(new Packets.HandshakeResponse(false));
            ctx.disconnect();
        }
    }

    @PacketHandler
    public void onLogin(Packets.LoginRequest packet, PacketProcessorContext ctx) {
        String username = packet.username();
        if (username.length() < 3 || username.length() > 16 || loggedInUsers.containsValue(username)) {
            ctx.send(new Packets.LoginResponse(packet.id(), false, "Invalid or taken username."));
            return;
        }

        loggedInUsers.put(ctx.nettyCtx().channel(), username);
        ctx.send(new Packets.LoginResponse(packet.id(), true, "Welcome to the server!"));
        ctx.switchState(Protocol.Play.class);
        System.out.println("User '" + username + "' logged in. Switching to PLAY state.");
        broadcast(new Packets.ChatMessageToClient("Server", username + " has joined the chat."));
    }

    @PacketHandler
    public void onChatMessage(Packets.ChatMessageToServer packet, PacketProcessorContext ctx) {
        String sender = loggedInUsers.get(ctx.nettyCtx().channel());
        if (sender != null) {
            System.out.println("[" + sender + "]: " + packet.message());
            broadcast(new Packets.ChatMessageToClient(sender, packet.message()));
        }
    }

    @PacketHandler
    public void onKeepAlive(Packets.KeepAlivePacket packet, PacketProcessorContext ctx) {

    }

    private void broadcast(Packets.ChatMessageToClient packet) {
        for (Channel channel : loggedInUsers.keySet()) {
            channel.writeAndFlush(packet);
        }
    }
}
