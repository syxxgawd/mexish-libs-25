package net.mexish.libs.commons.util;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.regex.Pattern;

/**
 * @author mexish
 * @version 10/11/2025
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SuppressWarnings("all")
@UtilityClass
public final class Strings {

    ThreadLocal<StringBuilder> CACHE = ThreadLocal.withInitial(StringBuilder::new);

    Pattern COLOR_CODE_PATTERN = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");

    DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###");

    public @NotNull StringBuilder getCachedEmptyBuilder() {
        val sb = CACHE.get();
        sb.setLength(0);

        return sb;
    }

    @Contract(value = " -> new", pure = true)
    public @NotNull StringBuilder newStringBuilder() {
        return new StringBuilder();
    }

    /**
     * This is the simple translate colors, this will NOT keep track of the last color used.
     */
    @Contract(pure = true)
    public @NotNull String simpleTranslateColors(final @NotNull String string) {
        return string.replace("&", "\u00A7");
    }

    /**
     * @return if text is a color (&f for example)
     */
    public boolean isColor(final @NonNull String text) {
        return COLOR_CODE_PATTERN.matcher(simpleTranslateColors(text)).matches();
    }

    public String fastFormatText(final @NonNull String text,
                                 final Object @NonNull ... o) {
        return formatText(text, getCachedEmptyBuilder(), o);
    }

    public String formatText(final @NonNull String text,
                             final Object @NonNull ... o) {
        return formatText(text, newStringBuilder(), o);
    }

    public String formatText(final @NonNull String text,
                             final @NonNull StringBuilder sb,
                             final Object @NonNull ... o) {
        if (o.length == 0) {
            return text;
        }

        var idx = 0;

        for (var i = 0; i < text.length(); i++) {
            val ch = text.charAt(i);

            if (ch == '{' && text.charAt(i + 1) == '}') {
                sb.append(idx >= o.length ? "" : o[idx++]);
                i++;

                continue;
            }

            sb.append(ch);
        }

        return sb.toString();
    }

    public @NotNull String formatNumber(final int number) {
        return DECIMAL_FORMAT.format(number);
    }

}
