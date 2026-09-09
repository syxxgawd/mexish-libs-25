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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class RequestManager {

    AtomicInteger idCounter = new AtomicInteger();
    Map<Integer, CompletableFuture<?>> pending = new ConcurrentHashMap<>();

    public <R extends Packet.Response> @NotNull CompletableFuture<R> createRequest(
            final @NonNull Packet.Request<R> packet,
            final @NonNull Channel channel) {
        return createRequest(packet, channel, 5, TimeUnit.SECONDS);
    }

    public <R extends Packet.Response> @NotNull CompletableFuture<R> createRequest(
            final @NonNull Packet.Request<R> packet,
            final @NonNull Channel channel,
            final long timeout,
            final @NonNull TimeUnit unit) {

        val id = idCounter.getAndIncrement();
        val future = new CompletableFuture<R>();

        pending.put(id, future);
        future.orTimeout(timeout, unit).whenComplete((v_, _v) -> pending.remove(id));

        channel.writeAndFlush(new Packet.Envelope(id, packet));
        return future;
    }

    @SuppressWarnings("unchecked")
    public boolean handleIncoming(final @NonNull Packet packet, final int requestId) {
        if (packet instanceof Packet.Response response && requestId != -1) {
            val future = (CompletableFuture<Packet.Response>) pending.remove(requestId);

            if (future != null) {
                future.complete(response);
                return true;
            }
        }

        return false;
    }

    public void clearAndFailPending() {
        idCounter.set(0);

        for (val future : pending.values()) {
            // maybe change to a diff exception
            future.completeExceptionally(new TimeoutException());
        }

        pending.clear();
    }

}
