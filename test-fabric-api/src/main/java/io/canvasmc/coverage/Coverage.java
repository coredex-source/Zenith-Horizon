package io.canvasmc.coverage;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.world.level.gamerules.GameRule;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.List;

public final class Coverage implements ModInitializer {
    private static final List<String> HOLDERS = List.of(
        "net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents",
        "net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents",
        "net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents",
        "net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents",
        "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents",
        "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents",
        "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents",
        "net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents",
        "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents",
        "net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents",
        "net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents",
        "net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents",
        "net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents",
        "net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents",
        "net.fabricmc.fabric.api.event.player.AttackBlockCallback",
        "net.fabricmc.fabric.api.event.player.AttackEntityCallback",
        "net.fabricmc.fabric.api.event.player.BlockEvents",
        "net.fabricmc.fabric.api.event.player.ItemEvents",
        "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents",
        "net.fabricmc.fabric.api.event.player.PlayerPickItemEvents",
        "net.fabricmc.fabric.api.event.player.UseBlockCallback",
        "net.fabricmc.fabric.api.event.player.UseEntityCallback",
        "net.fabricmc.fabric.api.event.player.UseItemCallback",
        "net.fabricmc.fabric.api.networking.v1.ClientboundConfigurationChannelEvents",
        "net.fabricmc.fabric.api.networking.v1.ClientboundPlayChannelEvents",
        "net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents",
        "net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents",
        "net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents",
        "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents",
        "net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent",
        "net.fabricmc.fabric.api.message.v1.ServerMessageEvents",
        "net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback",
        "net.fabricmc.fabric.api.advancement.v1.AdvancementEvents",
        "net.fabricmc.fabric.api.block.v1.FluidFlowEvents",
        "net.fabricmc.fabric.api.dimension.v1.DimensionEvents",
        "net.fabricmc.fabric.api.gamerule.v1.GameRuleEvents",
        "net.fabricmc.fabric.api.item.v1.EnchantmentEvents",
        "net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents",
        "net.fabricmc.fabric.api.item.v1.BlockTransformerEvents",
        "net.fabricmc.fabric.api.item.v1.ItemClickBehaviorCallback",
        "net.fabricmc.fabric.api.loot.v3.LootTableEvents",
        "net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback",
        "net.fabricmc.fabric.api.permission.v1.PermissionEvents"
    );

    @Override
    public void onInitialize() {
        Recorder recorder = Recorder.open(FabricLoader.getInstance().getGameDir().resolve("fabric-api-coverage.txt"));
        int registered = 0;
        for (String holder : HOLDERS) {
            registered += register(recorder, holder);
        }
        registered += registerGameRules(recorder);
        recorder.comment("listening to " + registered + " events");
        if (FabricLoader.getInstance().isModLoaded("fabric-command-api-v2")) {
            CoverageCommands.register(recorder);
        }
    }

    @SuppressWarnings("unchecked")
    private static int registerGameRules(@NonNull Recorder recorder) {
        Class<?> type;
        Method changeCallback;
        Class<?> listener;
        try {
            type = Class.forName("net.fabricmc.fabric.api.gamerule.v1.GameRuleEvents", true, Coverage.class.getClassLoader());
            changeCallback = type.getMethod("changeCallback", GameRule.class);
            listener = Class.forName("net.fabricmc.fabric.api.gamerule.v1.GameRuleEvents$ValueUpdate", true, Coverage.class.getClassLoader());
        } catch (ReflectiveOperationException | LinkageError exception) {
            return 0;
        }

        int registered = 0;
        for (GameRule<?> rule : BuiltInRegistries.GAME_RULE) {
            String name = "GameRuleEvents." + BuiltInRegistries.GAME_RULE.getKey(rule).getPath();
            try {
                Event<Object> event = (Event<Object>) changeCallback.invoke(null, rule);
                event.register(Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[]{listener}, new Listener(recorder, name)));
                registered++;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                recorder.comment("couldn't listen to " + name + ": " + exception);
            }
        }
        return registered;
    }

    @SuppressWarnings("unchecked")
    private static int register(@NonNull Recorder recorder, @NonNull String holder) {
        Class<?> type;
        try {
            type = Class.forName(holder, true, Coverage.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError exception) {
            return 0;
        }

        int registered = 0;
        for (Field field : type.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !Event.class.isAssignableFrom(field.getType())) continue;
            if (!(field.getGenericType() instanceof ParameterizedType generic) || !(generic.getActualTypeArguments()[0] instanceof Class<?> listener)) continue;

            String name = type.getSimpleName() + "." + field.getName();
            try {
                Event<Object> event = (Event<Object>) field.get(null);
                event.register(listener == ChatDecorator.class
                    ? DecoratorListener.create(recorder, name)
                    : Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[]{listener}, new Listener(recorder, name)));
                registered++;
            } catch (Throwable exception) {
                recorder.comment("couldn't listen to " + name + ": " + exception);
            }
        }
        return registered;
    }
}
