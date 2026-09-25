package io.canvasmc.horizon.fabric;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PaperOverrides {
    public static final String DEFAULTS_FILE = "paper-mod.defaults.json";
    public static final String OVERRIDES_FILE = "paper-mod.json";

    private static final String PATCHES = "fabric/patches/";
    private static final String REMOVE_PREFIX = "-";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Logger logger;
    private final String source;
    private int changes;

    private PaperOverrides(@NonNull Logger logger, @NonNull String source) {
        this.logger = logger;
        this.source = source;
    }

    public static @NonNull ObjectNode load(@NonNull Path launchDirectory, @NonNull Logger logger) {
        Path directory = launchDirectory.resolve("config").resolve("horizon");
        Path overridesFile = directory.resolve(OVERRIDES_FILE);
        ObjectNode merged = readDefaults();

        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(DEFAULTS_FILE), MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(merged));
            if (Files.notExists(overridesFile)) {
                Files.writeString(overridesFile, "{\n}\n");
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Couldn't write the Paper overrides to " + directory, exception);
        }

        PaperOverrides overrides = new PaperOverrides(logger, launchDirectory.relativize(overridesFile).toString());
        overrides.merge(merged, overrides.read(overridesFile));
        if (overrides.changes > 0) {
            logger.info("Applied {} change(s) to the Paper overrides from {}", overrides.changes, overrides.source);
        }
        return merged;
    }

    private static @NonNull ObjectNode readDefaults() {
        JarFile jar = HorizonLoader.getInternalPlugin().file().jarFile();
        List<JarEntry> patches = jar.stream()
            .filter((entry) -> entry.getName().startsWith(PATCHES) && entry.getName().endsWith(".json"))
            .sorted(Comparator.comparing(JarEntry::getName))
            .toList();

        ObjectNode defaults = MAPPER.createObjectNode();
        for (JarEntry patch : patches) {
            try (InputStream input = jar.getInputStream(patch)) {
                combine(defaults, (ObjectNode) MAPPER.readTree(input));
            } catch (IOException | ClassCastException exception) {
                throw new IllegalStateException("Couldn't read " + patch.getName(), exception);
            }
        }
        return defaults;
    }

    private static void combine(@NonNull ObjectNode target, @NonNull ObjectNode source) {
        for (Map.Entry<String, JsonNode> entry : source.properties()) {
            JsonNode current = target.get(entry.getKey());
            if (current instanceof ObjectNode object && entry.getValue() instanceof ObjectNode other) {
                combine(object, other);
            }
            else if (current instanceof ArrayNode array && entry.getValue() instanceof ArrayNode other) {
                other.forEach((element) -> {
                    if (!contains(array, element)) array.add(element);
                });
            }
            else {
                target.set(entry.getKey(), entry.getValue().deepCopy());
            }
        }
    }

    private @NonNull ObjectNode read(@NonNull Path file) {
        JsonNode node;
        try {
            node = MAPPER.readTree(file.toFile());
        } catch (JsonProcessingException exception) {
            JsonLocation location = exception.getLocation();
            throw new IllegalArgumentException("Invalid " + source + " at line " + location.getLineNr()
                + ", column " + location.getColumnNr() + ": " + exception.getOriginalMessage());
        } catch (IOException exception) {
            throw new UncheckedIOException("Couldn't read " + source, exception);
        }

        if (node == null || node.isMissingNode()) {
            return MAPPER.createObjectNode();
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("Invalid " + source + ": the file must contain a JSON object");
        }
        return (ObjectNode) node;
    }

    private void merge(@NonNull ObjectNode target, @NonNull ObjectNode overrides) {
        for (Map.Entry<String, JsonNode> entry : overrides.properties()) {
            String key = entry.getKey();
            JsonNode override = entry.getValue();
            JsonNode current = target.get(key);

            if (override.isNull()) {
                if (target.remove(key) != null) changes++;
            }
            else if (current == null) {
                target.set(key, override);
                changes++;
            }
            else if (current.isObject() && override.isObject()) {
                merge((ObjectNode) current, (ObjectNode) override);
            }
            else if (current.isArray() && override.isArray()) {
                mergeArray(key, (ArrayNode) current, (ArrayNode) override);
            }
            else if (!current.isContainerNode() && !override.isContainerNode()) {
                if (!current.equals(override)) changes++;
                target.set(key, override);
            }
            else {
                throw new IllegalArgumentException("Invalid " + source + ": \"" + key + "\" must be "
                    + (current.isObject() ? "an object" : current.isArray() ? "an array" : "a single value")
                    + " to change the default");
            }
        }
    }

    private void mergeArray(@NonNull String key, @NonNull ArrayNode target, @NonNull ArrayNode overrides) {
        for (JsonNode element : overrides) {
            if (element.isTextual() && element.textValue().startsWith(REMOVE_PREFIX)) {
                String removed = element.textValue().substring(REMOVE_PREFIX.length());
                if (removeText(target, removed)) {
                    changes++;
                }
                else {
                    logger.warn("{} removes \"{}\" from \"{}\", but it is not in the defaults", source, removed, key);
                }
            }
            else if (!contains(target, element)) {
                target.add(element);
                changes++;
            }
        }
    }

    private static boolean removeText(@NonNull ArrayNode array, @NonNull String text) {
        boolean removed = false;
        for (int i = array.size() - 1; i >= 0; i--) {
            JsonNode element = array.get(i);
            if (element.isTextual() && element.textValue().equals(text)) {
                array.remove(i);
                removed = true;
            }
        }
        return removed;
    }

    private static boolean contains(@NonNull ArrayNode array, @NonNull JsonNode element) {
        for (JsonNode existing : array) {
            if (existing.equals(element)) return true;
        }
        return false;
    }
}
