package io.canvasmc.horizon.fabric.mixin;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricMixinConfigs {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "mixin_preflight");
    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
        .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
        .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
        .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
        .build();
    private static final List<String> MIXIN_KEYS = List.of("mixins", "server");

    private static final Map<String, String> MODS = new ConcurrentHashMap<>();
    private static final Map<String, Entry> BY_NAME = new ConcurrentHashMap<>();
    private static final Map<String, Entry> BY_PACKAGE = new ConcurrentHashMap<>();
    private static final Map<String, Entry> BY_MIXIN = new ConcurrentHashMap<>();
    private static final Map<String, IMixinConfig> CONFIGS = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> DISABLED = new ConcurrentHashMap<>();
    private static final Set<String> SKIPPED = ConcurrentHashMap.newKeySet();

    private FabricMixinConfigs() {
    }

    public static void register(@NonNull String config, @NonNull String modId) {
        MODS.putIfAbsent(config, modId);
    }

    public static void disable(@NonNull JsonNode disabledMixins) {
        disabledMixins.properties().forEach((mod) -> {
            Set<String> mixins = DISABLED.computeIfAbsent(mod.getKey(), (key) -> ConcurrentHashMap.newKeySet());
            mod.getValue().forEach((mixin) -> mixins.add(mixin.asText()));
        });
    }

    public static void warnUnmatched() {
        DISABLED.forEach((mod, mixins) -> {
            if (!FabricLoader.getInstance().isModLoaded(mod)) return;
            mixins.stream()
                .filter((mixin) -> !SKIPPED.contains(mixin))
                .sorted()
                .forEach((mixin) -> LOGGER.warn("The default overrides disable mixin {} from {}, but {} has no such mixin", mixin, mod, mod));
        });
    }

    public static void capture() {
        for (Config config : Mixins.getConfigs()) {
            if (MODS.containsKey(config.getName())) CONFIGS.put(config.getName(), config.getConfig());
        }
    }

    public static @Nullable IMixinConfig config(@NonNull String name) {
        return CONFIGS.get(name);
    }

    public static @Nullable String modId(@NonNull String config) {
        return MODS.get(config);
    }

    public static @Nullable Entry ofMixin(@NonNull String internalName) {
        return BY_MIXIN.get(internalName);
    }

    public static @Nullable Entry claim(@NonNull String mixinPackage) {
        return BY_PACKAGE.get(mixinPackage);
    }

    public static @NonNull Collection<Entry> entries() {
        return BY_NAME.values();
    }

    public static @Nullable InputStream rewrite(@NonNull String name, @Nullable InputStream stream) {
        String modId = MODS.get(name);
        if (modId == null || stream == null) {
            return stream;
        }

        byte[] bytes;
        try (stream) {
            bytes = stream.readAllBytes();
        } catch (IOException exception) {
            LOGGER.warn(exception, "Couldn't read mixin config {} from {}", name, modId);
            return null;
        }

        try {
            JsonNode root = MAPPER.readTree(bytes);
            if (!(root instanceof ObjectNode config) || !config.path("package").isTextual()) {
                return new ByteArrayInputStream(bytes);
            }

            String mixinPackage = config.get("package").asText();
            removeSkipped(config, mixinPackage, modId);
            String plugin = config.path("plugin").isTextual() ? config.get("plugin").asText() : null;
            Entry entry = new Entry(
                name, modId, mixinPackage, plugin,
                config.path("required").asBoolean(false),
                config.path("injectors").path("defaultRequire").asInt(0),
                mixinNames(config, mixinPackage)
            );

            BY_NAME.put(name, entry);
            entry.mixins().forEach((mixin) -> BY_MIXIN.put(mixin.replace('.', '/'), entry));
            Entry existing = BY_PACKAGE.putIfAbsent(mixinPackage, entry);
            if (existing == null || existing.name().equals(name)) {
                config.put("plugin", HorizonMixinConfigPlugin.class.getName());
            }
            else {
                LOGGER.debug("Mixin config {} shares the package {} with {}, skipping preflight for the mod.", name, mixinPackage, existing.name());
            }
            return new ByteArrayInputStream(MAPPER.writeValueAsBytes(config));
        } catch (IOException exception) {
            LOGGER.warn("Couldn't parse mixin config {} from {}, skipping the preflight for it: {}", name, modId, exception.getMessage());
            return new ByteArrayInputStream(bytes);
        }
    }

    private static void removeSkipped(@NonNull ObjectNode config, @NonNull String mixinPackage, @NonNull String modId) {
        Set<String> disabled = DISABLED.getOrDefault(modId, Set.of());
        for (String key : MIXIN_KEYS) {
            if (!(config.get(key) instanceof ArrayNode mixins)) continue;

            for (int i = mixins.size() - 1; i >= 0; i--) {
                String mixin = mixinPackage + "." + mixins.get(i).asText();
                String entry = disabledBy(disabled, mixin);
                if (entry != null) {
                    LOGGER.info("Skipping mixin {} from {}, the default overrides disable it", mixin, modId);
                    SKIPPED.add(entry);
                    mixins.remove(i);
                }
                else if (MixinQuarantine.skip(mixin)) {
                    mixins.remove(i);
                }
            }
        }
    }

    private static @Nullable String disabledBy(@NonNull Set<String> disabled, @NonNull String mixin) {
        if (disabled.contains(mixin)) {
            return mixin;
        }

        for (String entry : disabled) {
            if (entry.endsWith(".*") && mixin.startsWith(entry.substring(0, entry.length() - 1))) return entry;
        }
        return null;
    }

    private static @NonNull List<String> mixinNames(@NonNull ObjectNode config, @NonNull String mixinPackage) {
        List<String> names = new ArrayList<>();
        for (String key : MIXIN_KEYS) {
            for (JsonNode mixin : config.path(key)) {
                if (mixin.isTextual()) names.add(mixinPackage + "." + mixin.asText());
            }
        }
        return names;
    }

    public record Entry(
        String name,
        String modId,
        String mixinPackage,
        @Nullable String plugin,
        boolean required,
        int defaultRequire,
        List<String> mixins
    ) {
    }
}
