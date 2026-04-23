package dev.vyrexen.creativecrafting.mixin.client;

import dev.vyrexen.creativecrafting.SlotColorConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends HandledScreen<ScreenHandler> {

    private static final int[] SLOT_X = { 172, 133, 151, 133, 151 };
    private static final int[] SLOT_Y = {  20,  10,  10,  28,  28 };

    @Shadow private static ItemGroup selectedTab;

    public CreativeInventoryScreenMixin(ScreenHandler handler) {
        super(handler, null, null);
    }

    // ── Slot positioning ──────────────────────────────────────────────────────

    @Inject(method = "setSelectedTab", at = @At("TAIL"))
    private void onSetSelectedTab(ItemGroup tab, CallbackInfo ci) {
        if (!tab.getType().equals(ItemGroup.Type.INVENTORY)) return;
        for (int i = 0; i < 5; i++) {
            Slot slot = handler.slots.get(i);
            slot.x = SLOT_X[i];
            slot.y = SLOT_Y[i];
        }
    }

    // ── Slot color rendering ──────────────────────────────────────────────────

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void onDrawBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!selectedTab.getType().equals(ItemGroup.Type.INVENTORY)) return;

        int[] colors = SlotColorConfig.getInstance().getColors();
        int shadow  = colors[0];
        int hilight = colors[1];
        int fill    = colors[2];

        for (int i = 0; i < 5; i++) {
            Slot slot = handler.slots.get(i);
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            context.fill(sx,      sy,      sx + 18, sy + 1,  shadow);
            context.fill(sx,      sy + 1,  sx + 1,  sy + 17, shadow);
            context.fill(sx + 17, sy + 1,  sx + 18, sy + 17, hilight);
            context.fill(sx,      sy + 17, sx + 18, sy + 18, hilight);
            context.fill(sx + 1,  sy + 1,  sx + 17, sy + 17, fill);
        }
    }

    // ── Key handling ──────────────────────────────────────────────────────────
    // SPACE        = craft once
    // SHIFT+SPACE  = craft all (mass craft, like shift-click on crafting table)
    // DELETE       = clear focused ingredient slot

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (!selectedTab.getType().equals(ItemGroup.Type.INVENTORY)) return;

        int key = input.getKeycode();

        if (key == InputUtil.GLFW_KEY_DELETE) {
            int slotIdx = handler.slots.indexOf(focusedSlot);
            if (slotIdx < 1 || slotIdx > 4 || focusedSlot.getStack().isEmpty()) return;
            focusedSlot.setStack(ItemStack.EMPTY);
            client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slotIdx, ItemStack.EMPTY));
            cir.setReturnValue(true);
            return;
        }

        if (key == InputUtil.GLFW_KEY_SPACE) {
            boolean shift = (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS)
                         || (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS);
            if (shift) {
                craftAll();
            } else {
                craftOnce();
            }
            cir.setReturnValue(true);
        }
    }

    // ── Mouse handling ────────────────────────────────────────────────────────
    // Left-click output  = craft once
    // Shift-click output = craft all

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (!selectedTab.getType().equals(ItemGroup.Type.INVENTORY)) return;
        if (slot == null || handler.slots.indexOf(slot) != 0) return;

        if (actionType == SlotActionType.QUICK_MOVE) {
            craftAll();
            ci.cancel();
        } else if (actionType == SlotActionType.PICKUP) {
            craftOnce();
            ci.cancel();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void craftOnce() {
        Slot output = handler.slots.get(0);
        if (output.getStack().isEmpty()) return;
        client.interactionManager.clickSlot(
                handler.syncId, 0, 0, SlotActionType.QUICK_MOVE, client.player);
        playCraftSound();
    }

    private void craftAll() {
        boolean crafted = false;
        for (int i = 0; i < 64; i++) {
            if (handler.slots.get(0).getStack().isEmpty()) break;
            client.interactionManager.clickSlot(
                    handler.syncId, 0, 0, SlotActionType.QUICK_MOVE, client.player);
            crafted = true;
        }
        if (crafted) playCraftSound();
    }

    private void playCraftSound() {
        client.player.playSound(
                SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
                SoundCategory.BLOCKS,
                1.0f, 1.0f);
    }
}
