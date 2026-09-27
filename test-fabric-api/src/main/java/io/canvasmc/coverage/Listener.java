package io.canvasmc.coverage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

record Listener(Recorder recorder, String event) implements InvocationHandler {
    private static final Set<String> HANDLED_WHEN_TRUE = Set.of("useCustomElytra", "setBedOccupationState");
    private static final Set<String> NEUTRAL_CONSTANTS = Set.of("PASS", "DEFAULT");

    @Override
    public @Nullable Object invoke(Object proxy, @NonNull Method method, Object @Nullable [] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "toString" -> "coverage listener for " + this.event;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> args != null && proxy == args[0];
                default -> null;
            };
        }
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }

        this.recorder.record(this.event, args);
        return neutral(method, args);
    }

    private static boolean nullable(@NonNull Method method) {
        for (Annotation annotation : method.getAnnotatedReturnType().getAnnotations()) {
            if (annotation.annotationType().getSimpleName().equals("Nullable")) return true;
        }
        return false;
    }

    private static @Nullable Object neutral(@NonNull Method method, Object @Nullable [] args) {
        Class<?> type = method.getReturnType();
        if (type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return !HANDLED_WHEN_TRUE.contains(method.getName());
        }
        if (method.getName().startsWith("replace") || nullable(method)) {
            return null;
        }
        if (type != Object.class && args != null) {
            for (int i = args.length - 1; i >= 0; i--) {
                if (type.isInstance(args[i])) return args[i];
            }
        }
        if (CompletableFuture.class.isAssignableFrom(type)) {
            return CompletableFuture.completedFuture(null);
        }
        for (Field field : type.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && type.isAssignableFrom(field.getType()) && NEUTRAL_CONSTANTS.contains(field.getName())) {
                try {
                    return field.get(null);
                } catch (IllegalAccessException exception) {
                    return null;
                }
            }
        }
        return null;
    }
}
