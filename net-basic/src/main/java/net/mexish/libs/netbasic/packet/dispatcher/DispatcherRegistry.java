package net.mexish.libs.netbasic.packet.dispatcher;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.commons.util.ModernReferenceMap;
import net.mexish.libs.commons.util.NuclearReferenceMap;
import net.mexish.libs.netbasic.packet.state.ProtocolState;

import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class DispatcherRegistry {

    Map<Class<? extends ProtocolState>, PacketDispatcher> dispatchers
            = NuclearReferenceMap.create(ModernReferenceMap.RefType.STRONG, ModernReferenceMap.RefType.STRONG);

    public void register(final @NonNull Class<? extends ProtocolState> state,
                         final @NonNull PacketDispatcher dispatcher) {
        dispatchers.put(state, dispatcher);
    }

    public PacketDispatcher get(final @NonNull Class<? extends ProtocolState> state) {
        return dispatchers.get(state);
    }

    public void loadAutomatically() {
        ServiceLoader.load(DispatcherLoader.class).forEach(loader -> loader.load(this));
    }

}
