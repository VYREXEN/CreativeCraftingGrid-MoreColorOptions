package stardust.cc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.logging.LogUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CreativeCrafting implements ClientModInitializer {
    public static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    public static final Logger LOGGER = LogUtils.getLogger();
    
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("creativecrafting.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static Config config = new Config(false);

    public static Config getConfig() {
        return config;
    }
    
    private static void loadConfig() {
        try {
            if (Files.exists(PATH)) {
                config = GSON.fromJson(Files.readString(PATH), Config.class);
                LOGGER.info("[CreativeCrafting] Loaded config: sticky={}", config.sticky);
            } else {
                saveConfig();
            }
        } catch (IOException e) {
            LOGGER.error("[CreativeCrafting] Failed to load config", e);
        }
    }
    
    private static void saveConfig() {
        try {
            Files.writeString(PATH, GSON.toJson(config));
            LOGGER.info("[CreativeCrafting] Saved config: sticky={}", config.sticky);
        } catch (IOException e) {
            LOGGER.error("[CreativeCrafting] Failed to save config", e);
        }
    }
    
    @Override
    public void onInitializeClient() {
        loadConfig();
        
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("creativecrafting")
                .then(ClientCommandManager.literal("sticky")
                    .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            config.sticky = BoolArgumentType.getBool(context, "enabled");
                            Text enabled = Text.literal(config.sticky ? "enabled" : "disabled").formatted(config.sticky ? Formatting.GREEN : Formatting.RED);
                            context.getSource().sendFeedback(Text.literal("Sticky mode ").append(enabled));
                            saveConfig();
                            return 1;
                        })
                    )
                    .executes(context -> {
                        Text enabled = Text.literal(config.sticky ? "enabled" : "disabled").formatted(config.sticky ? Formatting.GREEN : Formatting.RED);
                        context.getSource().sendFeedback(Text.literal("Sticky mode is currently ").append(enabled));
                        return 1;
                    })
                )
            );
        });
    }

    public static class Config {
        private boolean sticky = false;

        public Config(boolean sticky) {
            this.sticky = sticky;
        }

        public boolean isSticky() {
            return sticky;
        }
    }
}