package stardust.cc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class CreativeCrafting implements ClientModInitializer {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final Logger logger = LogUtils.getLogger();
    
    private static boolean sticky = false;
    
    public static boolean isSticky() {
        return sticky;
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("creativecrafting")
                .then(ClientCommandManager.literal("sticky")
                    .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            sticky = BoolArgumentType.getBool(context, "enabled");
                            Text enabled = sticky ? Text.literal("enabled").formatted(Formatting.GREEN) : Text.literal("disabled").formatted(Formatting.RED);
                            context.getSource().sendFeedback(Text.literal("Sticky mode ").append(enabled));
                            return 1;
                        })
                    )
                    .executes(context -> {
                        Text enabled = sticky ? Text.literal("enabled").formatted(Formatting.GREEN) : Text.literal("disabled").formatted(Formatting.RED);
                        context.getSource().sendFeedback(Text.literal("Sticky mode is currently ").append(enabled));
                        return 1;
                    })
                )
            );
        });
    }
}