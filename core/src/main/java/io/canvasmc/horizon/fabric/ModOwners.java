package io.canvasmc.horizon.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.ModContainerImpl;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;

public final class ModOwners {
    private static volatile @Nullable Map<Path, ModContainer> owners;

    private ModOwners() {
    }

    public static @Nullable ModContainer of(@NonNull Class<?> type) {
        CodeSource source = type.getProtectionDomain().getCodeSource();
        if (source == null || source.getLocation() == null) {
            return null;
        }

        try {
            return owners().get(Path.of(source.getLocation().toURI()).toAbsolutePath().normalize());
        } catch (Exception exception) {
            return null;
        }
    }

    private static @NonNull Map<Path, ModContainer> owners() {
        Map<Path, ModContainer> map = owners;
        if (map == null) {
            map = new HashMap<>();
            for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                if (mod instanceof ModContainerImpl impl) {
                    for (Path path : impl.getCodeSourcePaths()) {
                        map.putIfAbsent(path.toAbsolutePath().normalize(), mod);
                    }
                }
            }
            owners = map;
        }
        return map;
    }
}
