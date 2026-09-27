package io.canvasmc.coverage;

import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

final class Recorder {
    private static final List<String> COUNTED = List.of(
        "ServerTickEvents.", "ServerChunkEvents.", "ServerEntityEvents.", "ServerBlockEntityEvents.", "EntityTrackingEvents.",
        "PermissionEvents.", "LootTableEvents.", "FluidFlowEvents.", "DefaultItemComponentEvents.", "AdvancementEvents.",
        "CommandRegistrationCallback.", "DynamicRegistrySetupCallback.", "EnchantmentEvents.", "BlockTransformerEvents.",
        "CommonLifecycleEvents."
    );

    private final BufferedWriter writer;
    private final Map<String, Integer> counts = new TreeMap<>();

    private Recorder(BufferedWriter writer) {
        this.writer = writer;
    }

    static @NonNull Recorder open(@NonNull Path file) {
        try {
            return new Recorder(Files.newBufferedWriter(file, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    synchronized void record(@NonNull String event, Object @Nullable [] args) {
        boolean player = args != null && args.length > 0 && args[0] instanceof Player;
        for (String prefix : COUNTED) {
            if (!player && event.startsWith(prefix)) {
                this.counts.merge(args == null || args.length == 0 ? event : event + " | " + Describe.kind(args[0]), 1, Integer::sum);
                return;
            }
        }

        StringJoiner line = new StringJoiner(" | ");
        line.add(event);
        if (args != null) {
            for (Object arg : args) line.add(Describe.of(arg));
        }
        write(line.toString());
    }

    synchronized void mark(@NonNull String section) {
        flushCounts();
        write("== " + section);
    }

    synchronized void comment(@NonNull String text) {
        write("# " + text);
    }

    synchronized void flushCounts() {
        this.counts.forEach((event, count) -> write("# count " + count + " " + event));
        this.counts.clear();
    }

    private void write(@NonNull String line) {
        try {
            this.writer.write(line);
            this.writer.newLine();
            this.writer.flush();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
