package io.canvasmc.testmod;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TestChunkAttachments {
    private static final AttachmentType<String> WORLDGEN = AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("horizon-testmod", "worldgen_mark"), Codec.STRING);
    private static final AtomicBoolean GENERATED = new AtomicBoolean();
    private static final AtomicBoolean LOADED = new AtomicBoolean();
    private static final AtomicBoolean MISSING = new AtomicBoolean();
    private static final String BOOT = "boot " + Long.toHexString(System.nanoTime()) + " ";
    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();

    private TestChunkAttachments() {
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            String mark = ((AttachmentTarget) chunk).getAttached(WORLDGEN);
            boolean first = SEEN.add(level.dimension().identifier() + " " + chunk.getPos());
            if (mark == null) {
                if (!MISSING.getAndSet(true)) {
                    TestMod.LOGGER.info("attachment: chunk {} has no worldgen mark (generated={})", chunk.getPos(), generated);
                }
            } else if (generated) {
                if (!GENERATED.getAndSet(true)) {
                    TestMod.LOGGER.info("attachment: worldgen mark reached the full chunk {} ({})", chunk.getPos(), mark);
                }
            } else if (first && !mark.startsWith(BOOT) && !LOADED.getAndSet(true)) {
                TestMod.LOGGER.info("attachment: worldgen mark from an earlier run loaded from disk with chunk {} ({})", chunk.getPos(), mark);
            }
        });
        TestMod.worldgenAttachments = true;
    }

    public static void mark(ChunkAccess chunk) {
        ((AttachmentTarget) chunk).setAttached(WORLDGEN, BOOT + "features at " + chunk.getPos());
    }
}
