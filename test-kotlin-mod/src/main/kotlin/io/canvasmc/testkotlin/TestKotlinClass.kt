package io.canvasmc.testkotlin

import net.fabricmc.api.ModInitializer

class TestKotlinClass : ModInitializer {
    override fun onInitialize() {
        TestKotlinMod.log("class entrypoint ran")
    }
}
