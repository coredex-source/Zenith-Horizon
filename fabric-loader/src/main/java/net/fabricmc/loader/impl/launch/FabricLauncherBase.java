package net.fabricmc.loader.impl.launch;

public abstract class FabricLauncherBase implements FabricLauncher {
    private static FabricLauncher launcher;

    protected FabricLauncherBase() {
        setLauncher(this);
    }

    public static void setLauncher(FabricLauncher launcher) {
        if (FabricLauncherBase.launcher != null && FabricLauncherBase.launcher != launcher) {
            throw new IllegalStateException("Fabric launcher has already been set");
        }
        FabricLauncherBase.launcher = launcher;
    }

    public static FabricLauncher getLauncher() {
        return launcher;
    }
}
