package stardust.cc.mixin.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeCraftingMixin extends HandledScreen<ScreenHandler> {
    @Unique private static final int OFFSET_X = 35;
    @Unique private static final int OFFSET_Y = -8;
    @Shadow private static ItemGroup selectedTab;

    public CreativeCraftingMixin(ScreenHandler handler) {
        super(handler, null, null);
    }

    @Unique private Slot getCraftingSlotAt(double mouseX, double mouseY) {
        for (int i = 0; i <= 4; i++) {
            Slot slot = client.player.playerScreenHandler.getSlot(i);
            int slotX = this.x + slot.x + OFFSET_X;
            int slotY = this.y + slot.y + OFFSET_Y;
            if (i == 0) slotX -= 17; // Shift the crafted item's slot over to fit in the inventory screen
            if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) return slot; // Check if mouse is within slot bounds
        }

        return null;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!selectedTab.getType().equals(ItemGroup.Type.INVENTORY)) return;

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        Slot slot = getCraftingSlotAt(mouseX, mouseY);
        if (slot != null) {
            ItemStack cursorStack = client.player.currentScreenHandler.getCursorStack();
            ItemStack slotStack = slot.getStack();
            if (button == 0 && click.hasShift()) {
                handleShiftClick(slot, slotStack);
            } else if (button == 0) {
                handleLeftClick(slot, cursorStack, slotStack);
            } else if (button == 1) {
                handleRightClick(slot, cursorStack, slotStack);
            } else if (button == 2) {
                handleMiddleClick(cursorStack, slotStack);
            }

            cir.setReturnValue(true);
        }
    }

    @Unique private void handleLeftClick(Slot slot, ItemStack cursorStack, ItemStack slotStack) {
        if (cursorStack.isEmpty() && !slotStack.isEmpty()) {
            // Pick up item from slot
            client.player.currentScreenHandler.setCursorStack(slotStack.copy());
            slot.setStack(ItemStack.EMPTY);
            client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot.id, ItemStack.EMPTY));
        } else if (!cursorStack.isEmpty() && slotStack.isEmpty()) {
            // Place cursor item into slot
            slot.setStack(cursorStack.copy());
            client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot.id, cursorStack));
            client.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
        } else if (!cursorStack.isEmpty() && !slotStack.isEmpty()) {
            if (ItemStack.areItemsAndComponentsEqual(cursorStack, slotStack)) {
                // Same item type, merge stacks
                int space = slotStack.getMaxCount() - slotStack.getCount();
                int toMove = Math.min(cursorStack.getCount(), space);
                if (toMove > 0) {
                    ItemStack newSlotStack = slotStack.copyWithCount(slotStack.getCount() + toMove);
                    ItemStack newCursor = cursorStack.copyWithCount(cursorStack.getCount() - toMove);
                    slot.setStack(newSlotStack);
                    client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot.id, newSlotStack));
                    client.player.currentScreenHandler.setCursorStack(newCursor.getCount() > 0 ? newCursor : ItemStack.EMPTY);
                }
            } else {
                // Different item types, swap
                slot.setStack(cursorStack.copy());
                client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot.id, cursorStack));
                client.player.currentScreenHandler.setCursorStack(slotStack.copy());
            }
        }
    }

    @Unique private void handleRightClick(Slot slot, ItemStack cursorStack, ItemStack slotStack) {
        if (!cursorStack.isEmpty()) {
            // Place one item from cursor
            if (slotStack.isEmpty()) {
                slot.setStack(cursorStack.copyWithCount(1));
                client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot.id, slot.getStack()));
                ItemStack newCursor = cursorStack.copyWithCount(cursorStack.getCount() - 1);
                client.player.currentScreenHandler.setCursorStack(newCursor.getCount() > 0 ? newCursor : ItemStack.EMPTY);
            } else if (ItemStack.areItemsAndComponentsEqual(cursorStack, slotStack) && slotStack.getCount() < slotStack.getMaxCount()) {
                ItemStack newSlotStack = slotStack.copyWithCount(slotStack.getCount() + 1);
                slot.setStack(newSlotStack);
                client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot.id, newSlotStack));
                ItemStack newCursor = cursorStack.copyWithCount(cursorStack.getCount() - 1);
                client.player.currentScreenHandler.setCursorStack(newCursor.getCount() > 0 ? newCursor : ItemStack.EMPTY);
            }
        } else if (!slotStack.isEmpty()) {
            // Pick up half
            int half = (slotStack.getCount() + 1) / 2;
            client.player.currentScreenHandler.setCursorStack(slotStack.copyWithCount(half));
            ItemStack remaining = slotStack.copyWithCount(slotStack.getCount() - half);
            slot.setStack(remaining.getCount() > 0 ? remaining : ItemStack.EMPTY);
            client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot.id, slot.getStack()));
        }
    }

    @Unique private void handleMiddleClick(ItemStack cursorStack, ItemStack slotStack) {
        if (cursorStack.isEmpty() && !slotStack.isEmpty()) {
            client.player.currentScreenHandler.setCursorStack(slotStack.copyWithCount(slotStack.getMaxCount()));
        }
    }

    @Unique private void handleShiftClick(Slot slot, ItemStack slotStack) {
        if (slotStack.isEmpty()) return;

        int remaining = slotStack.getCount();

        // First pass: fill partial stacks of the same item in main inventory (9-35) then hotbar (36-44)
        for (int i = 9; i <= 44 && remaining > 0; i++) {
            Slot targetSlot = client.player.playerScreenHandler.getSlot(i);
            ItemStack targetStack = targetSlot.getStack();
            if (!targetStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(slotStack, targetStack)) {
                int space = targetStack.getMaxCount() - targetStack.getCount();
                if (space > 0) {
                    int toMove = Math.min(remaining, space);
                    ItemStack newTargetStack = targetStack.copyWithCount(targetStack.getCount() + toMove);
                    targetSlot.setStack(newTargetStack);
                    client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(i, newTargetStack));
                    remaining -= toMove;
                }
            }
        }

        // Second pass: place remainder in empty slots
        for (int i = 9; i <= 44 && remaining > 0; i++) {
            Slot targetSlot = client.player.playerScreenHandler.getSlot(i);
            if (targetSlot.getStack().isEmpty()) {
                int toPlace = Math.min(remaining, slotStack.getMaxCount());
                ItemStack newStack = slotStack.copyWithCount(toPlace);
                targetSlot.setStack(newStack);
                client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(i, newStack));
                remaining -= toPlace;
            }
        }

        // Update source slot
        if (remaining <= 0) {
            slot.setStack(ItemStack.EMPTY);
            client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot.id, ItemStack.EMPTY));
        } else {
            ItemStack remainingStack = slotStack.copyWithCount(remaining);
            slot.setStack(remainingStack);
            client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot.id, remainingStack));
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
        if (!selectedTab.getType().equals(ItemGroup.Type.INVENTORY)) return;
        if (getCraftingSlotAt(click.x(), click.y()) != null) cir.setReturnValue(true); // Handle drag release on crafting slots if needed
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (!selectedTab.getType().equals(ItemGroup.Type.INVENTORY)) return;
        if (input.getKeycode() != GLFW.GLFW_KEY_DELETE) return;

        // Delete key pressed, delete item in hovered slot
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        Slot slot = getCraftingSlotAt(mouseX, mouseY);
        if (slot != null && !slot.getStack().isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
            client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot.id, ItemStack.EMPTY));
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!selectedTab.getType().equals(ItemGroup.Type.INVENTORY)) return;

        for (int i = 0; i <= 4; i++) {
            Slot slot = client.player.playerScreenHandler.getSlot(i);
            int screenX = this.x + slot.x + OFFSET_X;
            int screenY = this.y + slot.y + OFFSET_Y;
            if (i == 0) screenX -= 17; // Shift product slot to fit in screen

            int slotX = screenX - 1, slotY = screenY - 1;
            context.fill(slotX, slotY, slotX + 18, slotY + 1, 0xFF373737);           // top edge
            context.fill(slotX, slotY + 1, slotX + 1, slotY + 17, 0xFF373737);       // left edge
            context.fill(slotX + 17, slotY + 1, slotX + 18, slotY + 17, 0xFFFFFFFF); // right edge
            context.fill(slotX, slotY + 17, slotX + 18, slotY + 18, 0xFFFFFFFF);     // bottom edge
            context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF8B8B8B);  // interior
            if (mouseX >= screenX && mouseX < screenX + 16 && mouseY >= screenY && mouseY < screenY + 16) {
                context.fill(screenX, screenY, screenX + 16, screenY + 16, 0x80FFFFFF);
            }

            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                context.drawItem(stack, screenX, screenY);
                context.drawStackOverlay(client.textRenderer, stack, screenX, screenY);
            }
        }

        // Re-render cursor stack on top of everything
        ItemStack cursorStack = client.player.currentScreenHandler.getCursorStack();
        if (!cursorStack.isEmpty()) {
            context.drawItem(cursorStack, mouseX - 8, mouseY - 8);
            context.drawStackOverlay(client.textRenderer, cursorStack, mouseX - 8, mouseY - 8);
        }
    }
}