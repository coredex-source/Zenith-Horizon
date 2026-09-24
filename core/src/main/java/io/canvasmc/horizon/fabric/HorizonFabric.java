package io.canvasmc.horizon.fabric;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.service.EmberClassLoader;
import io.canvasmc.horizon.util.MinecraftVersion;
import io.canvasmc.horizon.util.ServerProperties;
import io.canvasmc.horizon.util.Util;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public final class HorizonFabric {
    public static final String MOD_METADATA = "fabric.mod.json";

    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "fabric");
    private static boolean loaded;

    private HorizonFabric() {
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void load(@NonNull EmberClassLoader classLoader, @NonNull Path gameJar, @NonNull String entrypoint, @NonNull List<Path> classPath, String @NonNull [] args) {
        HorizonLoader horizon = HorizonLoader.getInstance();
        ServerProperties properties = horizon.getProperties();

        List<Path> mods = findMods(properties);
        if (mods.isEmpty()) {
            LOGGER.debug("No Fabric mods found, skipping Fabric loader.");
            return;
        }

        MinecraftVersion minecraftVersion = horizon.getVersionMeta().minecraftVersion();
        if (minecraftVersion.isOlderThan(MinecraftVersion.FABRIC_MINIMUM)) {
            String found = mods.stream()
                .map((mod) -> mod.getFileName().toString())
                .sorted()
                .collect(Collectors.joining(", "));
            throw new IllegalStateException("Fabric mods require Minecraft >=" + MinecraftVersion.FABRIC_MINIMUM.getName()
                + " but the server is on " + minecraftVersion.getName() + ". Remove these mods to continue: " + found);
        }

        setPropertyIfAbsent(SystemProperties.MODS_FOLDER, properties.modsDirectory().getAbsolutePath());
        if (!properties.extraMods().isEmpty()) {
            setPropertyIfAbsent(SystemProperties.ADD_MODS, properties.extraMods().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator)));
        }

        Log.init(new HorizonFabricLogHandler(LOGGER));

        HorizonGameProvider provider = new HorizonGameProvider(
            horizon.getVersionMeta(), List.of(gameJar), entrypoint, Path.of("").toAbsolutePath(), args
        );
        HorizonFabricLauncher launcher = new HorizonFabricLauncher(classLoader, classPath, entrypoint);

        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        loader.setGameProvider(provider);
        provider.initialize(launcher);

        try {
            loader.load();
            loader.freeze();
        } catch (FormattedException exception) {
            throw Util.kill(exception.getMainText(), exception.getMessage() != null ? exception : exception.getCause());
        }

        loaded = true;
    }

    private static @NonNull List<Path> findMods(@NonNull ServerProperties properties) {
        List<Path> jars = new ArrayList<>();
        properties.extraMods().forEach((file) -> jars.add(file.toPath()));

        Path modsDirectory = properties.modsDirectory().toPath();
        if (Files.isDirectory(modsDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                modsDirectory, path -> Files.isRegularFile(path) && path.toString().endsWith(".jar"))) {
                stream.forEach(jars::add);
            } catch (IOException e) {
                throw new RuntimeException("Failed to scan mods directory", e);
            }
        }

        return jars.stream().filter(HorizonFabric::isMod).toList();
    }

    private static boolean isMod(@NonNull Path jar) {
        try (JarFile file = new JarFile(jar.toFile())) {
            return file.getEntry(MOD_METADATA) != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static void setPropertyIfAbsent(@NonNull String key, @NonNull String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
