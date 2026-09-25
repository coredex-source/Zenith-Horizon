package io.canvasmc.horizon.fabric.mixin;

import org.jspecify.annotations.NonNull;

public enum PreflightPolicy {
    FAIL("fail"),
    DISABLE_MIXIN("disable-mixin"),
    WARN("warn");

    private final String id;

    PreflightPolicy(@NonNull String id) {
        this.id = id;
    }

    public static @NonNull PreflightPolicy byId(@NonNull String id) {
        for (PreflightPolicy policy : values()) {
            if (policy.id.equalsIgnoreCase(id.trim())) return policy;
        }
        throw new IllegalArgumentException("Unknown mixinPreflight policy '" + id + "', expected fail, disable-mixin or warn");
    }

    public @NonNull String id() {
        return id;
    }
}
