package io.canvasmc.testmod;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class TestServerMod implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        TestMod.LOGGER.info("server entrypoint (class)");

        try {
            Field random = MinecraftServer.class.getDeclaredField("random");
            Method logFullTickTime = MinecraftServer.class.getDeclaredMethod("logFullTickTime");
            TestMod.LOGGER.info("class tweaker: random public={} final={}, logFullTickTime public={}, injected interface={}",
                Modifier.isPublic(random.getModifiers()), Modifier.isFinal(random.getModifiers()),
                Modifier.isPublic(logFullTickTime.getModifiers()),
                ServerExtension.class.isAssignableFrom(MinecraftServer.class));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }

        TestMod.LOGGER.info("environment stripping: client method present={}, client field present={}, client interface present={}, frames={}",
            Arrays.stream(TestEnvironment.class.getDeclaredMethods()).anyMatch((method) -> method.getName().equals("clientOnly")),
            Arrays.stream(TestEnvironment.class.getDeclaredFields()).anyMatch((field) -> field.getName().equals("clientField")),
            ClientMarker.class.isAssignableFrom(TestEnvironment.class),
            TestEnvironment.pick(false).getClass().getSimpleName());
    }
}
