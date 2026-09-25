package io.canvasmc.testmod;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvironmentInterface;

@EnvironmentInterface(value = EnvType.CLIENT, itf = ClientMarker.class)
public final class TestEnvironment implements ClientMarker {
    @Environment(EnvType.CLIENT)
    public static String clientField = "client";

    private TestEnvironment() {
    }

    @Environment(EnvType.CLIENT)
    public static String clientOnly() {
        return "client";
    }

    public static Object pick(boolean first) {
        return first ? new First() : new Second();
    }

    public static final class First {
    }

    public static final class Second {
    }
}
