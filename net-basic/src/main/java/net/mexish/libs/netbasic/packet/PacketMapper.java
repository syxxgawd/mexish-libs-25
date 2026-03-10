package net.mexish.libs.netbasic.packet;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PacketMapper {

    PacketFactory[] idFactoryMap
            = new PacketFactory[256];

    Map<Class<?>, Integer> classIdMap
            = new ConcurrentHashMap<>(256, 0.5F);

    public void registerPacket(final int id,
                               final @NonNull Class<?> cls,
                               final @NonNull PacketFactory factory) {
        idFactoryMap[id] = factory;
        classIdMap.put(cls, id);
    }

    public @Nullable Packet newPacket(final int id, final @NotNull ByteBuf buf) {
        if (id < 0 || id >= idFactoryMap.length) {
            return null;
        }

        val factory = idFactoryMap[id];
        if (factory == null) return null;

        return factory.create(buf);
    }

    public int getPacketId(final @NonNull Class<?> cls) {
        return classIdMap.getOrDefault(cls, -1);
    }

}
