package net.mexish.libs.netbasic.packet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class RequestManager {

    AtomicInteger idCounter = new AtomicInteger(1);
    Map<Integer, CompletableFuture<Packet.Response>> pending = new ConcurrentHashMap<>();

    public @NotNull CompletableFuture<Packet.Response> createRequest(final @NonNull Packet.Request packet,
                                                                     final @NonNull Channel channel) {
        val id = idCounter.getAndIncrement();
        val future = new CompletableFuture<Packet.Response>();

        pending.put(id, future);
        // TODO maybe not always 5 seconds??
        future.orTimeout(5, TimeUnit.SECONDS).whenComplete((v_, _v) -> pending.remove(id));

        channel.writeAndFlush(new Packet.Envelope(id, packet));
        return future;
    }

    public boolean handleIncoming(final @NonNull Packet packet, final int requestId) {
        if (packet instanceof Packet.Response response && requestId != -1) {
            val future = pending.remove(requestId);
            if (future != null) {
                future.complete(response);
                return true;
            }
        }

        return false;
    }
}
