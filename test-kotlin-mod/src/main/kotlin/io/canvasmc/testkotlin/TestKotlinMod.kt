package io.canvasmc.testkotlin

import com.mojang.brigadier.Command
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory
import kotlin.reflect.full.declaredMemberProperties

object TestKotlinMod : ModInitializer {
    private val logger = LoggerFactory.getLogger("horizon-kotlin")

    val server = DedicatedServerModInitializer {
        log("property entrypoint ran")
    }

    override fun onInitialize() {
        log("object entrypoint ran")

        val squares = runBlocking {
            (1..4).map { async(Dispatchers.Default) { it * it } }.sumOf { it.await() }
        }
        log("coroutines: $squares")

        log("reflect: ${TestKotlinMod::class.declaredMemberProperties.map { it.name }.sorted()}")

        val json = buildJsonObject {
            put("loader", "horizon")
            put("adapter", "kotlin")
        }
        log("serialization: ${Json.encodeToString(JsonObject.serializer(), json)}")

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            log("server started with ${server.playerCount} players")
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(Commands.literal("kotlintest").executes { context ->
                context.source.sendSuccess({ Component.literal("kotlin command works") }, false)
                Command.SINGLE_SUCCESS
            })
        }
    }

    fun log(message: String) {
        logger.info("kotlin: $message")
    }
}

fun onMain() {
    TestKotlinMod.log("function entrypoint ran")
}
