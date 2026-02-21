package net.mexish.libs.netbasic.packet.registry;

import lombok.NonNull;
import net.mexish.libs.commons.util.ModernReferenceMap;
import net.mexish.libs.commons.util.NuclearReferenceMap;
import net.mexish.libs.netbasic.packet.ProtocolMapping;
import net.mexish.libs.netbasic.packet.state.ProtocolState;

import java.util.Map;

/**
 * @author mexish
 */
public enum ProtocolRegistry {
    INSTANCE;

    private final Map<Class<? extends ProtocolState>, ProtocolMapping> stateMappings
            = NuclearReferenceMap.create(ModernReferenceMap.RefType.STRONG, ModernReferenceMap.RefType.STRONG);

    public void register(final @NonNull Class<? extends ProtocolState> state,
                         final @NonNull ProtocolMapping mapping) {
        stateMappings.put(state, mapping);
    }

    public ProtocolMapping get(final @NonNull Class<? extends ProtocolState> state) {
        return stateMappings.get(state);
    }
}
