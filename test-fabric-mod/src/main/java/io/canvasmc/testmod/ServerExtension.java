package io.canvasmc.testmod;

public interface ServerExtension {
    default String testmod$injected() {
        return "injected";
    }
}
