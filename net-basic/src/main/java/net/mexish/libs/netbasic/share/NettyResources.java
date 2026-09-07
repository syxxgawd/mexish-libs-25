package net.mexish.libs.netbasic.share;


import io.netty.channel.EventLoopGroup;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.commons.util.ModernReferenceMap;
import net.mexish.libs.commons.util.NuclearReferenceMap;
import net.mexish.libs.netbasic.util.TransportUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum NettyResources {
    INSTANCE;

    Map<TransportUtils.TransportType, EventLoopGroup> workerCache = new ConcurrentHashMap<>();
    Map<TransportUtils.TransportType, EventLoopGroup> bossCache = new ConcurrentHashMap<>();

    public EventLoopGroup getSharedWorker(final @NonNull TransportUtils.TransportType type) {
        return workerCache.computeIfAbsent(type,
                k -> TransportUtils.newEventLoopGroup(k, 0, "netty-worker-" + k.name().toLowerCase()));
    }

    public EventLoopGroup getSharedBoss(final @NonNull TransportUtils.TransportType type) {
        return bossCache.computeIfAbsent(type,
                k -> TransportUtils.newEventLoopGroup(k, 1, "netty-boss-" + k.name().toLowerCase()));
    }

    public void shutdown() {
        workerCache.values().forEach(EventLoopGroup::shutdownGracefully);
        bossCache.values().forEach(EventLoopGroup::shutdownGracefully);
    }
}
