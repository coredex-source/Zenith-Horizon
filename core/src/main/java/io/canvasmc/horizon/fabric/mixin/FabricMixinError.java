package io.canvasmc.horizon.fabric.mixin;

import org.jspecify.annotations.NonNull;

public final class FabricMixinError extends Error {
    public FabricMixinError(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }
}
