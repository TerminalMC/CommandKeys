package com.notryken.commandkeys.util;

import com.notryken.commandkeys.CommandKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Localization {
    public static String translationKey(String domain, String path) {
        return domain + "." + CommandKeys.MOD_ID + "." + path;
    }

    public static MutableComponent localized(String domain, String path, Object... args) {
        return Component.translatable(translationKey(domain, path), args);
    }
}
