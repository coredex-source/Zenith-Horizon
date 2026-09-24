package io.canvasmc.horizon.fabric;

import io.canvasmc.horizon.service.EmberClassLoader;
import io.canvasmc.horizon.service.transform.TransformPhase;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.util.ManifestUtil;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.jar.Manifest;

public final class HorizonFabricLauncher extends FabricLauncherBase {
    private final EmberClassLoader classLoader;
    private final List<Path> classPath;
    private final String entrypoint;

    public HorizonFabricLauncher(@NonNull EmberClassLoader classLoader, @NonNull List<Path> classPath, @NonNull String entrypoint) {
        this.classLoader = classLoader;
        this.classPath = List.copyOf(classPath);
        this.entrypoint = entrypoint;
    }

    @Override
    public void addToClassPath(Path path, String... allowedPrefixes) {
        classLoader.addTransformationPath(path);
    }

    @Override
    public void setAllowedPrefixes(Path path, String... prefixes) {
    }

    @Override
    public void setValidParentClassPath(Collection<Path> paths) {
    }

    @Override
    public EnvType getEnvironmentType() {
        return EnvType.SERVER;
    }

    @Override
    public boolean isClassLoaded(String name) {
        return classLoader.hasClass(name);
    }

    @Override
    public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
        return classLoader.loadClass(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return classLoader.getResourceAsStream(name);
    }

    @Override
    public ClassLoader getTargetClassLoader() {
        return classLoader;
    }

    @Override
    public byte[] getClassByteArray(String name, boolean runTransformers) throws IOException {
        byte[] bytes;
        try (InputStream input = classLoader.getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (input == null) {
                return null;
            }
            bytes = input.readAllBytes();
        }
        return runTransformers ? classLoader.transformer.transformBytes(name, bytes, TransformPhase.INITIALIZE) : bytes;
    }

    @Override
    public Manifest getManifest(Path originPath) {
        try {
            return ManifestUtil.readManifest(originPath);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public String getEntrypoint() {
        return entrypoint;
    }

    @Override
    public List<Path> getClassPath() {
        return classPath;
    }
}
