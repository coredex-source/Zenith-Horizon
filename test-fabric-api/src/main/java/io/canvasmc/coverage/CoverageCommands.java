package io.canvasmc.coverage;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

final class CoverageCommands {
    private CoverageCommands() {
    }

    static void register(@NonNull Recorder recorder) {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal("coverage")
            .then(Commands.literal("mark").then(Commands.argument("section", StringArgumentType.word()).executes((command) -> {
                String section = StringArgumentType.getString(command, "section");
                recorder.mark(section);
                command.getSource().sendSuccess(() -> Component.literal("coverage section " + section), false);
                return 1;
            })))
            .then(Commands.literal("flush").executes((command) -> {
                recorder.flushCounts();
                command.getSource().sendSuccess(() -> Component.literal("coverage flushed"), false);
                return 1;
            }))));
    }
}
