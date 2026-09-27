package io.canvasmc.horizon.fabric;

import com.fasterxml.jackson.databind.JsonNode;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public final class PluginConflicts {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "plugin_conflicts");
    private static final List<String> DESCRIPTORS = List.of("paper-plugin.yml", "plugin.yml");
    private static final Pattern NAME = Pattern.compile("^name:\\s*[\"']?([^\"'#\\s]+)", Pattern.MULTILINE);
    private static final Pattern KOTLIN_LIBRARY = Pattern.compile("org\\.jetbrains\\.kotlinx?:([\\w.-]+):([\\w.+-]+)");
    private static final List<String> KOTLIN_GROUPS = List.of("org_jetbrains_kotlin_", "org_jetbrains_kotlinx_");
    private static final String KOTLIN_VERSION = "kotlin/KotlinVersionCurrentValue.class";
    private static final String COROUTINES_VERSION = "META-INF/kotlinx_coroutines_core.version";

    private PluginConflicts() {
    }

    public static void check(@NonNull JsonNode conflicts, @NonNull Path pluginsDirectory, @NonNull FabricLoader loader) {
        if (!Files.isDirectory(pluginsDirectory)) {
            return;
        }

        List<Plugin> plugins = plugins(pluginsDirectory);
        if (conflicts.isObject()) {
            checkMods(conflicts, plugins, loader);
        }
        checkKotlin(plugins, loader);
    }

    private static void checkMods(@NonNull JsonNode conflicts, @NonNull List<Plugin> plugins, @NonNull FabricLoader loader) {
        Map<String, Plugin> byName = new HashMap<>();
        plugins.forEach((plugin) -> byName.put(plugin.name().toLowerCase(Locale.ROOT), plugin));
        conflicts.properties().forEach((conflict) -> {
            Optional<ModContainer> mod = loader.getModContainer(conflict.getKey());
            if (mod.isEmpty()) return;

            for (JsonNode name : conflict.getValue().path("plugins")) {
                Plugin plugin = byName.get(name.asText().toLowerCase(Locale.ROOT));
                if (plugin != null) {
                    LOGGER.warn("{} is installed both as a Fabric mod and as the plugin {} ({}): {}",
                        mod.get().getMetadata().getName(), name.asText(), plugin.jar().getFileName(), conflict.getValue().path("reason").asText("keep only one of them"));
                }
            }
        });
    }

    private static void checkKotlin(@NonNull List<Plugin> plugins, @NonNull FabricLoader loader) {
        Map<String, ModContainer> provided = new HashMap<>();
        for (ModContainer mod : loader.getAllMods()) {
            String id = mod.getMetadata().getId();
            for (String group : KOTLIN_GROUPS) {
                if (id.startsWith(group)) provided.put(artifact(id.substring(group.length())), mod);
            }
        }
        if (provided.isEmpty()) {
            return;
        }

        for (Plugin plugin : plugins) {
            for (KotlinLibrary library : plugin.kotlin()) {
                ModContainer mod = provided.get(artifact(library.artifact()));
                if (mod == null) continue;

                Version ours = mod.getMetadata().getVersion();
                int comparison;
                try {
                    comparison = Version.parse(library.version()).compareTo(ours);
                } catch (VersionParsingException exception) {
                    continue;
                }
                if (comparison == 0) continue;

                String provider = mod.getContainingMod().orElse(mod).getMetadata().getName();
                String use = library.shaded() ? "shades" : "asks for";
                if (comparison > 0) {
                    LOGGER.warn("{} ({}) {} {} {}, but runs on the older {} from {}: plugins load Kotlin from the server first, so it may break",
                        plugin.name(), plugin.jar().getFileName(), use, library.artifact(), library.version(), ours.getFriendlyString(), provider);
                } else {
                    LOGGER.info("{} ({}) {} {} {}, but runs on the newer {} from {}, since plugins load Kotlin from the server first",
                        plugin.name(), plugin.jar().getFileName(), use, library.artifact(), library.version(), ours.getFriendlyString(), provider);
                }
            }
        }
    }

    private static @NonNull String artifact(@NonNull String artifact) {
        return artifact.endsWith("-jvm") ? artifact.substring(0, artifact.length() - 4) : artifact;
    }

    private static @NonNull List<Plugin> plugins(@NonNull Path pluginsDirectory) {
        List<Plugin> plugins = new ArrayList<>();
        try (Stream<Path> files = Files.list(pluginsDirectory)) {
            files.filter((file) -> file.getFileName().toString().endsWith(".jar")).forEach((jar) -> {
                try (JarFile file = new JarFile(jar.toFile())) {
                    for (String descriptor : DESCRIPTORS) {
                        ZipEntry entry = file.getEntry(descriptor);
                        if (entry == null) continue;

                        String text = read(file, entry);
                        Matcher name = NAME.matcher(text);
                        if (name.find()) plugins.add(new Plugin(name.group(1), jar, kotlin(file, text)));
                        break;
                    }
                } catch (IOException exception) {
                    LOGGER.debug("Couldn't read plugin {}: {}", jar.getFileName(), exception.getMessage());
                }
            });
        } catch (IOException exception) {
            LOGGER.debug("Couldn't list plugins in {}: {}", pluginsDirectory, exception.getMessage());
        }
        return plugins;
    }

    private static @NonNull List<KotlinLibrary> kotlin(@NonNull JarFile file, @NonNull String descriptor) throws IOException {
        List<KotlinLibrary> libraries = new ArrayList<>();
        Matcher declared = KOTLIN_LIBRARY.matcher(descriptor);
        while (declared.find()) {
            libraries.add(new KotlinLibrary(declared.group(1), declared.group(2), false));
        }

        ZipEntry stdlib = file.getEntry(KOTLIN_VERSION);
        if (stdlib != null) {
            try (InputStream input = file.getInputStream(stdlib)) {
                String version = kotlinVersion(input.readAllBytes());
                if (version != null) libraries.add(new KotlinLibrary("kotlin-stdlib", version, true));
            }
        }

        ZipEntry coroutines = file.getEntry(COROUTINES_VERSION);
        if (coroutines != null) {
            libraries.add(new KotlinLibrary("kotlinx-coroutines-core", read(file, coroutines).trim(), true));
        }
        return libraries;
    }

    private static @Nullable String kotlinVersion(byte @NonNull [] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        for (MethodNode method : node.methods) {
            List<Integer> numbers = new ArrayList<>();
            for (AbstractInsnNode insn : method.instructions) {
                int opcode = insn.getOpcode();
                if (opcode >= Opcodes.ICONST_0 && opcode <= Opcodes.ICONST_5) {
                    numbers.add(opcode - Opcodes.ICONST_0);
                } else if (insn instanceof IntInsnNode number && (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH)) {
                    numbers.add(number.operand);
                } else if (insn instanceof MethodInsnNode call && call.owner.equals("kotlin/KotlinVersion") && call.name.equals("<init>")
                    && call.desc.equals("(III)V") && numbers.size() >= 3) {
                    List<Integer> parts = numbers.subList(numbers.size() - 3, numbers.size());
                    return parts.get(0) + "." + parts.get(1) + "." + parts.get(2);
                }
            }
        }
        return null;
    }

    private static @NonNull String read(@NonNull JarFile file, @NonNull ZipEntry entry) throws IOException {
        try (InputStream input = file.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record Plugin(String name, Path jar, List<KotlinLibrary> kotlin) {
    }

    private record KotlinLibrary(String artifact, String version, boolean shaded) {
    }
}
