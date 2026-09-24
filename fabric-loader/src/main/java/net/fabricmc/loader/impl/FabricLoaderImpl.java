package net.fabricmc.loader.impl;

import net.fabricmc.loader.api.FabricLoader;

public abstract class FabricLoaderImpl implements FabricLoader {
    public static final String VERSION = "0.19.5";
    public static final String MOD_ID = "fabricloader";

    public static FabricLoaderImpl INSTANCE;

    protected FabricLoaderImpl() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Fabric loader has already been instantiated");
        }
        INSTANCE = this;
    }
}
