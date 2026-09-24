package io.canvasmc.horizon.fabric;

import io.canvasmc.horizon.util.PaperclipVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.minecraft.McVersionLookup;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ModDependencyImpl;
import net.fabricmc.loader.impl.util.Arguments;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class HorizonGameProvider implements GameProvider {
    private static final Set<BuiltinTransform> GAME_TRANSFORMS = EnumSet.of(BuiltinTransform.WIDEN_ALL_PACKAGE_ACCESS, BuiltinTransform.CLASS_TWEAKS);
    private static final Set<BuiltinTransform> MOD_TRANSFORMS = EnumSet.of(BuiltinTransform.STRIP_ENVIRONMENT);
    private static final List<String> GAME_PACKAGES = List.of(
        "net.minecraft.", "com.mojang.blaze3d.", "com.mojang.math.",
        "org.bukkit.", "org.spigotmc.", "io.papermc.", "com.destroystokyo.paper."
    );

    private final PaperclipVersion version;
    private final List<Path> gameJars;
    private final String entrypoint;
    private final Path launchDirectory;
    private final Arguments arguments;

    public HorizonGameProvider(@NonNull PaperclipVersion version, @NonNull List<Path> gameJars, @NonNull String entrypoint, @NonNull Path launchDirectory, String @NonNull [] args) {
        this.version = version;
        this.gameJars = List.copyOf(gameJars);
        this.entrypoint = entrypoint;
        this.launchDirectory = launchDirectory;
        this.arguments = new Arguments();
        this.arguments.parse(args);
    }

    @Override
    public String getGameId() {
        return "minecraft";
    }

    @Override
    public String getGameName() {
        return "Minecraft";
    }

    @Override
    public String getRawGameVersion() {
        return version.id();
    }

    @Override
    public String getNormalizedGameVersion() {
        return McVersionLookup.normalizeVersion(version.id(), McVersionLookup.getRelease(version.id()));
    }

    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        BuiltinModMetadata.Builder metadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
            .setName(getGameName());

        try {
            metadata.addDependency(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "java", List.of(String.format(Locale.ENGLISH, ">=%d", version.java_version()))));
        } catch (VersionParsingException exception) {
            throw new RuntimeException(exception);
        }

        return List.of(new BuiltinMod(gameJars, metadata.build()));
    }

    @Override
    public String getEntrypoint() {
        return entrypoint;
    }

    @Override
    public Path getLaunchDirectory() {
        return launchDirectory;
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return false;
    }

    @Override
    public Set<BuiltinTransform> getBuiltinTransforms(String className) {
        for (String gamePackage : GAME_PACKAGES) {
            if (className.startsWith(gamePackage)) return GAME_TRANSFORMS;
        }
        return MOD_TRANSFORMS;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean locateGame(FabricLauncher launcher, String[] args) {
        return true;
    }

    @Override
    public void initialize(FabricLauncher launcher) {
    }

    @Override
    public GameTransformer getEntrypointTransformer() {
        return new GameTransformer();
    }

    @Override
    public void unlockClassPath(FabricLauncher launcher) {
    }

    @Override
    public void launch(ClassLoader loader) {
        throw new UnsupportedOperationException("Horizon launches the server itself");
    }

    @Override
    public Arguments getArguments() {
        return arguments;
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        return arguments.toArray();
    }

    @Override
    public boolean canOpenErrorGui() {
        return false;
    }
}
