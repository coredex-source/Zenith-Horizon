package io.canvasmc.horizon.fabric;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.MixinLaunch;
import io.canvasmc.horizon.fabric.mixin.FabricMixinConfigs;
import io.canvasmc.horizon.fabric.mixin.MixinQuarantine;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.service.EmberClassLoader;
import io.canvasmc.horizon.util.MinecraftVersion;
import io.canvasmc.horizon.util.ServerProperties;
import io.canvasmc.horizon.util.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.game.minecraft.Hooks;
import net.fabricmc.loader.impl.launch.FabricMixinBootstrap;
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
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class HorizonFabric {
    public static final String MOD_METADATA = "fabric.mod.json";

    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "fabric");
    private static boolean loaded;
    private static Path launchDirectory;

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

        launchDirectory = Path.of("").toAbsolutePath();
        ObjectNode paperOverrides;
        try {
            paperOverrides = PaperOverrides.load(launchDirectory, LOGGER);
        } catch (IllegalArgumentException exception) {
            throw Util.kill(exception.getMessage(), null);
        }

        HorizonGameProvider provider = new HorizonGameProvider(
            horizon.getVersionMeta(), List.of(gameJar), entrypoint, launchDirectory, args, paperOverrides
        );
        HorizonFabricLauncher launcher = new HorizonFabricLauncher(classLoader, classPath, entrypoint);

        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        loader.setGameProvider(provider);
        provider.initialize(launcher);

        try {
            loader.load();
            loader.freeze();
        } catch (FormattedException exception) {
            throw fail(exception);
        }

        try {
            loader.loadClassTweakers();
        } catch (RuntimeException exception) {
            throw Util.kill("Couldn't load Fabric class tweakers", exception);
        }

        warnDuplicateClasses(loader);

        if (properties.mixinQuarantine()) {
            MixinQuarantine.load(launchDirectory);
        }

        loaded = true;
    }

    public static void invokePreLaunch() {
        if (!loaded) {
            return;
        }

        try {
            FabricLoaderImpl.INSTANCE.invokeEntrypoints("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
        } catch (RuntimeException exception) {
            throw fail(FormattedException.ofLocalized("exception.initializerFailure", exception));
        }
    }

    public static void startServer() {
        if (!loaded) {
            return;
        }

        Hooks.startServer(launchDirectory.toFile(), null);
    }

    public static void setGameInstance(@NonNull Object gameInstance) {
        if (!loaded) {
            return;
        }

        Hooks.setGameInstance(gameInstance);
    }

    private static @NonNull InternalError fail(@NonNull FormattedException exception) {
        return Util.kill(exception.getMainText(), exception.getMessage() != null ? exception : exception.getCause());
    }

    public static void bootstrapMixins() {
        if (!loaded) {
            return;
        }

        for (ModContainerImpl mod : FabricLoaderImpl.INSTANCE.getModsInternal()) {
            for (String config : mod.getMetadata().getMixinConfigs(EnvType.SERVER)) {
                FabricMixinConfigs.register(config, mod.getMetadata().getId());
            }
        }

        FabricMixinBootstrap.init(EnvType.SERVER, FabricLoaderImpl.INSTANCE);
        FabricMixinConfigs.capture();
    }

    private static void warnDuplicateClasses(@NonNull FabricLoaderImpl loader) {
        ClassLoader server = ClassLoader.getSystemClassLoader();
        for (ModContainerImpl mod : loader.getModsInternal()) {
            if (isBuiltin(mod)) continue;

            Map<String, Integer> packages = new TreeMap<>();
            for (Path root : mod.getRootPaths()) {
                try (Stream<Path> files = Files.walk(root)) {
                    files.map((file) -> root.relativize(file).toString().replace(File.separatorChar, '/'))
                        .filter((name) -> name.endsWith(".class") && !name.startsWith("META-INF/") && !name.endsWith("-info.class"))
                        .filter((name) -> isTransformable(name.substring(0, name.length() - 6).replace('/', '.')))
                        .filter((name) -> server.getResource(name) != null)
                        .forEach((name) -> packages.merge(name.contains("/") ? name.substring(0, name.lastIndexOf('/')).replace('/', '.') : "(default)", 1, Integer::sum));
                } catch (IOException exception) {
                    LOGGER.debug(exception, "Couldn't scan {} for duplicate classes", root);
                }
            }

            if (packages.isEmpty()) continue;

            String owner = mod.getContainingMod()
                .map((parent) -> mod.getMetadata().getId() + " (in " + parent.getMetadata().getId() + ")")
                .orElse(mod.getMetadata().getId());
            LOGGER.warn("{} ships {} class/classes that the server already has, using {} instead.", owner,
                packages.values().stream().mapToInt(Integer::intValue).sum(),
                packages.entrySet().stream().map((entry) -> entry.getKey() + " (" + entry.getValue() + ")").collect(Collectors.joining(", ")));
        }
    }

    private static boolean isBuiltin(@NonNull ModContainer mod) {
        if (mod.getMetadata().getType().equals("builtin")) return true;
        return mod.getContainingMod().map(HorizonFabric::isBuiltin).orElse(false);
    }

    private static boolean isTransformable(@NonNull String className) {
        return EmberClassLoader.EXCLUDE_PACKAGES.stream().noneMatch(className::startsWith)
            && !MixinLaunch.TRANSFORMATION_EXCLUDED_PATTERN.matcher(className).matches();
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
