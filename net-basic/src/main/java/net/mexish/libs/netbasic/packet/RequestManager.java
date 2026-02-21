package net.mexish.libs.netbasic.packet;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import net.mexish.libs.commons.util.ModernReferenceMap;
import net.mexish.libs.commons.util.NuclearReferenceMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class RequestManager {

    AtomicInteger idCounter = new AtomicInteger(1);
    Map<Integer, CompletableFuture<Packet.Response>> pending
            = NuclearReferenceMap.create(ModernReferenceMap.RefType.STRONG, ModernReferenceMap.RefType.STRONG);

    public @NotNull CompletableFuture<Packet.Response> createRequest(final @NonNull Packet.Request packet,
                                                                     final @NonNull Consumer<Packet> sender) {
        val id = idCounter.getAndIncrement();
        val future = new CompletableFuture<Packet.Response>();

        pending.put(id, future);
        future.orTimeout(5, TimeUnit.SECONDS).whenComplete((_, _) -> pending.remove(id));

        sender.accept(new Packet.Envelope(id, packet));
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
