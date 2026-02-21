package net.mexish.libs.netbasic.util;

import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@UtilityClass
public final class PacketUtils {

    public void writeString(ByteBuf buf, @NotNull String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public @NotNull InetAddress readAddress(@NotNull ByteBuf buf) throws UnknownHostException {
        byte[] bytes = new byte[4];
        buf.readBytes(bytes);

        return InetAddress.getByAddress(bytes);
    }

    public void writeAddress(@NotNull ByteBuf buf, @NotNull InetAddress address) {
        byte[] bytes = address.getAddress();
        buf.writeBytes(bytes);
    }

    public <T extends Enum<T>> T readEnum(ByteBuf buf, Class<T> cls) {
        return readEnum(buf, cls, false);
    }

    public <T extends Enum<T>> T readEnum(ByteBuf buf, Class<T> cls, boolean safe) {
        try {
            return safe
                    ? Enum.valueOf(cls, readString(buf))
                    : cls.getEnumConstants()[buf.readUnsignedByte()];
        } catch (Throwable t) {
            System.err.printf("Cannot find an enum %s [%s]: %s%n", cls, safe, t);
            return null;
        }
    }

    public void writeEnum(ByteBuf buf, Enum<?> e, boolean safe) {
        if (safe) {
            writeString(buf, e.name());
        } else {
            buf.writeByte(e.ordinal());
        }
    }

    public void writeEnum(ByteBuf buf, Enum<?> e) {
        writeEnum(buf, e, false);
    }

    public <T, C extends Collection<T>> C readCollection(ByteBuf buf, @NotNull IntFunction<C> colFactory, Supplier<T> reader) {
        int size = readVarInt(buf);

        C col = colFactory.apply(size);

        for (int i = 0; i < size; i++) {
            T element = reader.get();

            if (element != null) {
                col.add(element);
            }
        }

        return col;
    }

    public <T> T[] readArray(ByteBuf buf, @NotNull IntFunction<T[]> arrayFactory, Supplier<T> reader) {
        int size = readVarInt(buf);

        T[] col = arrayFactory.apply(size);

        for (int i = 0; i < size; i++) {
            T element = reader.get();

            if (element != null) {
                col[i] = element;
            }
        }

        return col;
    }

    public <T> void writeArray(ByteBuf buf, T @NotNull [] array, Consumer<T> writer) {
        writeVarInt(buf, array.length);

        for (T element : array) {
            writer.accept(element);
        }
    }

    public int[] readVarIntArray(@NotNull ByteBuf buf) {
        int[] array = new int[buf.readUnsignedByte()];

        for (int i = 0; i < array.length; i++) {
            array[i] = readVarInt(buf);
        }

        return array;
    }

    public void writeVarIntArray(@NotNull ByteBuf buf, int @NotNull [] array) {
        buf.writeByte(array.length);

        for (int i : array) {
            writeVarInt(buf, i);
        }
    }

    public <T> void writeCollection(ByteBuf buf, @NotNull Collection<T> collection, Consumer<T> writer) {
        writeVarInt(buf, collection.size());

        for (T element : collection) {
            writer.accept(element);
        }
    }

    public int[] readIntArray(@NotNull ByteBuf buf) {
        int[] array = new int[buf.readUnsignedByte()];

        for (int i = 0; i < array.length; i++) {
            array[i] = buf.readInt();
        }

        return array;
    }

    public void writeIntArray(@NotNull ByteBuf buf, int @NotNull [] array) {
        buf.writeByte(array.length);

        for (int i : array) {
            buf.writeInt(i);
        }
    }

    public String readString(ByteBuf buf) {
        int size = readVarInt(buf);

        if (size < 0) {
            throw new IllegalStateException("Received wrong string size");
        }

        if (size > buf.readableBytes()) {
            throw new IllegalArgumentException(size + " > " + buf.readableBytes());
        }

        byte[] bytes = new byte[size];
        buf.readBytes(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public int getVarIntSize(int input) {
        return (input & 0xFFFFFF80) == 0
                ? 1 : (input & 0xFFFFC000) == 0
                ? 2 : (input & 0xFFE00000) == 0
                ? 3 : (input & 0xF0000000) == 0
                ? 4 : 5;
    }

    public int readVarInt(@NotNull ByteBuf buf) {
        int result = 0;
        int numRead = 0;

        byte read;

        do {
            read = buf.readByte();
            result |= (read & 127) << numRead++ * 7;

            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 128) == 128);

        return result;
    }

    public void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        buf.writeByte(value);
    }

    public void writeByteArray(final ByteBuf buf, final byte @NotNull [] arr) {
        writeVarInt(buf, arr.length);
        buf.writeBytes(arr);
    }

    public byte @NotNull [] readByteArray(final ByteBuf buf) {
        val arr = new byte[readVarInt(buf)];
        buf.readBytes(arr);
        return arr;
    }

}
