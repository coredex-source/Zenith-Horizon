package io.canvasmc.horizon.fabric.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.throwables.MixinApplyError;
import org.spongepowered.asm.mixin.throwables.MixinPrepareError;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public final class MixinQuarantine {
    public static final String FILE = "config/horizon/mixin-quarantine.json";

    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "mixin_quarantine");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> MIXIN_KEYS = List.of("mixins", "server", "client");

    private static final Map<String, Quarantined> ENTRIES = new TreeMap<>();
    private static @Nullable Path file;

    private MixinQuarantine() {
    }

    public static synchronized void load(@NonNull Path launchDirectory) {
        file = launchDirectory.resolve(FILE);
        if (Files.notExists(file)) {
            return;
        }

        JsonNode root;
        try {
            root = MAPPER.readTree(file.toFile());
        } catch (IOException exception) {
            LOGGER.warn("Couldn't read {}, ignoring it: {}", FILE, exception.getMessage());
            return;
        }

        boolean changed = false;
        for (Map.Entry<String, JsonNode> property : root.properties()) {
            JsonNode node = property.getValue();
            String mod = node.path("mod").asText("");
            String version = node.path("version").asText("");
            Optional<String> installed = version(mod);
            if (installed.isEmpty()) {
                LOGGER.info("Removing quarantined mixin {}, as {} isn't installed anymore", property.getKey(), mod);
                changed = true;
                continue;
            }
            if (!installed.get().equals(version)) {
                LOGGER.info("Retrying quarantined mixin {}, as {} changed from {} to {}", property.getKey(), mod, version, installed.get());
                changed = true;
                continue;
            }

            ENTRIES.put(property.getKey(), new Quarantined(
                mod, version, node.path("config").asText(""),
                node.hasNonNull("target") ? node.get("target").asText() : null,
                node.path("reason").asText("")
            ));
        }

        if (changed) {
            save();
        }
        if (!ENTRIES.isEmpty()) {
            LOGGER.warn("{} mixin(s) are quarantined and will be skipped, remove them from {} to try them again", ENTRIES.size(), FILE);
        }
    }

    public static synchronized void filter(@NonNull ObjectNode config, @NonNull String mixinPackage) {
        if (ENTRIES.isEmpty()) {
            return;
        }

        for (String key : MIXIN_KEYS) {
            if (!(config.get(key) instanceof ArrayNode mixins)) continue;

            for (int i = mixins.size() - 1; i >= 0; i--) {
                String mixin = mixinPackage + "." + mixins.get(i).asText();
                Quarantined quarantined = ENTRIES.get(mixin);
                if (quarantined == null) continue;

                mixins.remove(i);
                LOGGER.warn("Skipping quarantined mixin {} from {}, as it failed on an earlier boot: {}", mixin, quarantined.mod(), quarantined.reason());
            }
        }
    }

    public static @NonNull Throwable failure(@NonNull String target, @NonNull Throwable thrown) {
        Failed failed = identify(thrown);
        if (failed == null) {
            return thrown;
        }

        boolean prepare = duringPrepare(thrown);
        String reason = reason(thrown);
        String failure = prepare ? "failed while preparing its config" : "failed to apply to " + target;
        LOGGER.error("Mixin {} from {} ({}) {}: {}", failed.mixin(), failed.mod(), failed.config(), failure, reason);

        if (HorizonLoader.getInstance().getProperties().mixinQuarantine()) {
            record(failed, prepare ? null : target, reason);
            LOGGER.error("Quarantined it, the next boot skips it. Remove it from {} to try it again", FILE);
        }
        else {
            LOGGER.error("Set mixinQuarantine to true in horizon.yml to skip failing mixins automatically on the next boot");
        }
        return new FabricMixinError("Mixin " + failed.mixin() + " from " + failed.mod() + " " + failure, thrown);
    }

    private static synchronized void record(@NonNull Failed failed, @Nullable String target, @NonNull String reason) {
        Quarantined quarantined = new Quarantined(failed.mod(), version(failed.mod()).orElse(""), failed.config(), target, reason);
        if (!quarantined.equals(ENTRIES.put(failed.mixin(), quarantined))) {
            save();
        }
    }

    private static void save() {
        if (file == null) {
            return;
        }

        ObjectNode root = MAPPER.createObjectNode();
        ENTRIES.forEach((mixin, quarantined) -> {
            ObjectNode node = root.putObject(mixin);
            node.put("mod", quarantined.mod());
            node.put("version", quarantined.version());
            node.put("config", quarantined.config());
            if (quarantined.target() != null) node.put("target", quarantined.target());
            node.put("reason", quarantined.reason());
        });

        try {
            Files.createDirectories(file.getParent());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
        } catch (IOException exception) {
            LOGGER.error(exception, "Couldn't write {}", FILE);
        }
    }

    private static @Nullable Failed identify(@NonNull Throwable thrown) {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (!(cause instanceof InvalidMixinException invalid) || invalid.getMixin() == null) continue;

            IMixinInfo mixin = invalid.getMixin();
            String config = mixin.getConfig().getName();
            String mod = FabricMixinConfigs.modId(config);
            return mod != null ? new Failed(mod, config, mixin.getClassName()) : null;
        }

        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message == null) continue;

            for (FabricMixinConfigs.Entry entry : FabricMixinConfigs.entries()) {
                int start = message.indexOf(entry.name() + ":");
                if (start < 0) continue;

                start += entry.name().length() + 1;
                int end = start;
                while (end < message.length() && !Character.isWhitespace(message.charAt(end))) end++;
                if (end > start) return new Failed(entry.modId(), entry.name(), entry.mixinPackage() + "." + message.substring(start, end));
            }
        }
        return null;
    }

    private static @NonNull String reason(@NonNull Throwable thrown) {
        Throwable reason = thrown;
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            reason = cause;
            if (cause instanceof InvalidMixinException || cause instanceof InjectionError) break;
        }

        String message = Objects.requireNonNullElse(reason.getMessage(), reason.getClass().getName());
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }

    private static boolean duringPrepare(@NonNull Throwable thrown) {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (cause instanceof MixinPrepareError) return true;
            if (cause instanceof MixinApplyError && cause.getMessage() != null && cause.getMessage().endsWith("during PREPARE")) return true;
        }
        return false;
    }

    private static @NonNull Optional<String> version(@NonNull String mod) {
        return FabricLoader.getInstance().getModContainer(mod).map((container) -> container.getMetadata().getVersion().getFriendlyString());
    }

    private record Failed(String mod, String config, String mixin) {
    }

    private record Quarantined(String mod, String version, String config, @Nullable String target, String reason) {
    }
}
