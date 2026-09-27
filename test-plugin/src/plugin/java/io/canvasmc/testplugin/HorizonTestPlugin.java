package io.canvasmc.testplugin;

import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.TreeSet;

public class HorizonTestPlugin extends JavaPlugin implements Listener {
    private static final String ECHO_CHANNEL = "horizon:plugin_echo";

    @Override
    public void onLoad() {
        getLogger().info("Test1: " + JavaPlugin.getPlugin(HorizonTestPlugin.class).getName());
        getLogger().info("Test2: " + JavaPlugin.getProvidingPlugin(TestBootstrapper.class).getName());
    }

    @Override
    public void onEnable() {
        getLogger().info("Hello!");
        getServer().getMessenger().registerOutgoingPluginChannel(this, ECHO_CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, ECHO_CHANNEL, (channel, player, message) -> {
            String text = new String(message, StandardCharsets.UTF_8);
            getLogger().info("plugin message '" + text + "' from " + player.getName());
            player.sendPluginMessage(this, ECHO_CHANNEL, ("echo " + text).getBytes(StandardCharsets.UTF_8));
        });
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onRegisterChannel(@NonNull PlayerRegisterChannelEvent event) {
        getLogger().info("channel registered: " + event.getChannel() + " by " + event.getPlayer().getName());
    }

    @EventHandler
    public void onChatDecorate(@NonNull AsyncChatDecorateEvent event) {
        String text = plain(event.result());
        getLogger().info("chat: bukkit decorate '" + text + "'");
        if (text.contains("bukkit-decorate")) {
            event.result(Component.text(text.replace("bukkit-decorate", "bukkit-decorated")));
        }
    }

    @EventHandler
    public void onChat(@NonNull AsyncChatEvent event) {
        String text = plain(event.message());
        getLogger().info("chat: bukkit chat '" + text + "' from " + event.getPlayer().getName());
        if (text.contains("bukkit-deny")) {
            event.setCancelled(true);
        }
    }

    private static @NonNull String plain(@NonNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @EventHandler
    public void onJoin(@NonNull PlayerJoinEvent event) {
        getServer().getScheduler().runTaskLater(this, () -> getLogger().info(
            "listening channels of " + event.getPlayer().getName() + ": " + new TreeSet<>(event.getPlayer().getListeningPluginChannels())), 20);
    }
}
