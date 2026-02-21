package net.mexish.libs.configuration.test.adapter;

import lombok.NonNull;
import lombok.val;
import net.mexish.libs.configuration.adapter.TypeAdapter;
import net.mexish.libs.configuration.test.type.Nigger;
import net.mexish.libs.configuration.type.Configuration;

/**
 * @author mexish
 */
public final class NiggerAdapter implements TypeAdapter<Nigger> {

    @Override
    public Nigger read(final @NonNull Configuration reader) {
        val nigger = new Nigger();
        nigger.setName(reader.getString("name"));
        nigger.setStatus(reader.getString("status"));

        return nigger;
    }

    @Override
    public void write(final @NonNull Configuration writer,
                      final @NonNull Nigger object) {
        writer.setString("name", object.getName());
        writer.setString("status", object.getStatus());
    }

}
