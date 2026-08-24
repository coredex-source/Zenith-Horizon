package io.canvasmc.horizon.inject;

import com.google.common.collect.ImmutableMap;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import me.lucko.spark.fabric.smap.MixinUtils;
import me.lucko.spark.fabric.smap.SourceMap;
import me.lucko.spark.fabric.smap.SourceMapProvider;
import me.lucko.spark.paper.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.paper.common.util.classfinder.ClassFinder;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * Modified version of {@link me.lucko.spark.fabric.FabricClassSourceLookup} to use Horizons API, and delegate to the
 * platform class source lookup to prevent issues with Bukkit/Paper plugins not showing up in the plugins tab
 */
public class HorizonClassSourceLookup extends ClassSourceLookup.ByCodeSource {
    private final ClassFinder classFinder;
    private final SourceMapProvider smapProvider;
    private final Path modsDirectory;
    private final Map<String, String> pathToModMap;

    private final ClassSourceLookup platformDelegate;

    public HorizonClassSourceLookup(ClassFinder classFinder, ClassSourceLookup platformDelegate) {
        this.classFinder = classFinder;
        this.platformDelegate = platformDelegate;
        this.smapProvider = new SourceMapProvider();

        final HorizonLoader horizon = HorizonLoader.getInstance();

        this.modsDirectory = horizon.getProperties().pluginsDirectory().toPath().toAbsolutePath().normalize();
        this.pathToModMap = constructPathToModIdMap(horizon.getPlugins().getAll());
    }

    @Override
    public String identifyFile(Path path) {
        String id = this.pathToModMap.get(path.toAbsolutePath().normalize().toString());
        if (id != null) {
            return id;
        }

        if (!path.startsWith(this.modsDirectory)) {
            return null;
        }

        return super.identifyFileName(this.modsDirectory.relativize(path).toString());
    }

    @Override
    public String identify(MethodCall methodCall) throws Exception {
        String className = methodCall.getClassName();
        String methodName = methodCall.getMethodName();
        String methodDesc = methodCall.getMethodDescriptor();

        if (className.equals("native") || methodName.equals("<init>") || methodName.equals("<clinit>")) {
            return platformDelegate.identify(methodCall);
        }

        Class<?> clazz = this.classFinder.findClass(className);
        if (clazz == null) {
            return platformDelegate.identify(methodCall);
        }

        Class<?>[] params = getParameterTypesForMethodDesc(methodDesc);
        Method reflectMethod = clazz.getDeclaredMethod(methodName, params);

        MixinMerged mixinMarker = reflectMethod.getDeclaredAnnotation(MixinMerged.class);
        if (mixinMarker == null) {
            return platformDelegate.identify(methodCall);
        }

        return modIdFromMixinClass(mixinMarker.mixin());
    }

    @Override
    public String identify(MethodCallByLine methodCall) throws Exception {
        String className = methodCall.getClassName();
        String methodName = methodCall.getMethodName();
        int lineNumber = methodCall.getLineNumber();

        if (className.equals("native") || methodName.equals("<init>") || methodName.equals("<clinit>")) {
            return platformDelegate.identify(methodCall);
        }

        SourceMap smap = this.smapProvider.getSourceMap(className);
        if (smap == null) {
            return platformDelegate.identify(methodCall);
        }

        int[] inputLineInfo = smap.getReverseLineMapping().get(lineNumber);
        if (inputLineInfo == null || inputLineInfo.length == 0) {
            return platformDelegate.identify(methodCall);
        }

        for (int fileInfoIds : inputLineInfo) {
            SourceMap.FileInfo inputFileInfo = smap.getFileInfo().get(fileInfoIds);
            if (inputFileInfo == null) {
                continue;
            }

            String path = inputFileInfo.path();
            if (path.endsWith(".java")) {
                path = path.substring(0, path.length() - 5);
            }

            String possibleMixinClassName = path.replace('/', '.');
            if (possibleMixinClassName.equals(className)) {
                continue;
            }

            return modIdFromMixinClass(possibleMixinClassName);
        }

        return null;
    }

    private static String modIdFromMixinClass(String mixinClassName) {
        for (Config config : MixinUtils.getMixinConfigs().values()) {
            IMixinConfig mixinConfig = config.getConfig();
            String mixinPackage = mixinConfig.getMixinPackage();
            if (!mixinPackage.isEmpty() && mixinClassName.startsWith(mixinPackage)) {
                return mixinConfig.getDecoration(FabricUtil.KEY_MOD_ID);
            }
        }
        return null;
    }

    private Class<?>[] getParameterTypesForMethodDesc(String methodDesc) {
        Type methodType = Type.getMethodType(methodDesc);
        Class<?>[] params = new Class[methodType.getArgumentTypes().length];
        Type[] argumentTypes = methodType.getArgumentTypes();

        for (int i = 0, argumentTypesLength = argumentTypes.length; i < argumentTypesLength; i++) {
            Type argumentType = argumentTypes[i];
            params[i] = getClassFromType(argumentType);
        }

        return params;
    }

    private Class<?> getClassFromType(Type type) {
        return switch (type.getSort()) {
            case Type.VOID -> void.class;
            case Type.BOOLEAN -> boolean.class;
            case Type.CHAR -> char.class;
            case Type.BYTE -> byte.class;
            case Type.SHORT -> short.class;
            case Type.INT -> int.class;
            case Type.FLOAT -> float.class;
            case Type.LONG -> long.class;
            case Type.DOUBLE -> double.class;
            case Type.ARRAY -> {
                final Class<?> classFromType = getClassFromType(type.getElementType());
                Class<?> result = classFromType;
                if (classFromType != null) {
                    for (int i = 0; i < type.getDimensions(); i++) {
                        result = result.arrayType();
                    }
                }
                yield result;
            }
            case Type.OBJECT -> this.classFinder.findClass(type.getClassName());
            default -> null;
        };
    }

    private static Map<String, String> constructPathToModIdMap(Collection<HorizonPlugin> mods) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (HorizonPlugin mod : mods) {
            String modId = mod.pluginMetadata().id();
            if (modId.equals("java")) {
                continue;
            }

            Path path = mod.file().ioFile().toPath();
            URI uri = path.toUri();
            if (uri.getScheme().equals("jar") && path.toString().equals("/")) { // ZipFileSystem
                String zipFilePath = path.getFileSystem().toString();
                builder.put(zipFilePath, modId);
            } else {
                builder.put(path.toAbsolutePath().normalize().toString(), modId);
            }
        }
        return builder.build();
    }
}
