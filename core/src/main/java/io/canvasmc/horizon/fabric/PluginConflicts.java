package io.canvasmc.horizon.fabric;

import com.fasterxml.jackson.databind.JsonNode;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private PluginConflicts() {
    }

    public static void check(@NonNull JsonNode conflicts, @NonNull Path pluginsDirectory, @NonNull FabricLoader loader) {
        if (!conflicts.isObject() || conflicts.isEmpty() || !Files.isDirectory(pluginsDirectory)) {
            return;
        }

        Map<String, Path> plugins = pluginNames(pluginsDirectory);
        conflicts.properties().forEach((conflict) -> {
            Optional<ModContainer> mod = loader.getModContainer(conflict.getKey());
            if (mod.isEmpty()) return;

            for (JsonNode plugin : conflict.getValue().path("plugins")) {
                Path jar = plugins.get(plugin.asText().toLowerCase(Locale.ROOT));
                if (jar != null) {
                    LOGGER.warn("{} is installed both as a Fabric mod and as the plugin {} ({}): {}",
                        mod.get().getMetadata().getName(), plugin.asText(), jar.getFileName(), conflict.getValue().path("reason").asText("keep only one of them"));
                }
            }
        });
    }

    private static @NonNull Map<String, Path> pluginNames(@NonNull Path pluginsDirectory) {
        Map<String, Path> names = new HashMap<>();
        try (Stream<Path> files = Files.list(pluginsDirectory)) {
            files.filter((file) -> file.getFileName().toString().endsWith(".jar")).forEach((jar) -> {
                try (JarFile file = new JarFile(jar.toFile())) {
                    for (String descriptor : DESCRIPTORS) {
                        ZipEntry entry = file.getEntry(descriptor);
                        if (entry == null) continue;
                        try (InputStream input = file.getInputStream(entry)) {
                            Matcher matcher = NAME.matcher(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                            if (matcher.find()) names.put(matcher.group(1).toLowerCase(Locale.ROOT), jar);
                        }
                        break;
                    }
                } catch (IOException exception) {
                    LOGGER.debug("Couldn't read plugin {}: {}", jar.getFileName(), exception.getMessage());
                }
            });
        } catch (IOException exception) {
            LOGGER.debug("Couldn't list plugins in {}: {}", pluginsDirectory, exception.getMessage());
        }
        return names;
    }
}
