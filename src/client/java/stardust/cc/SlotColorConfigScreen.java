package stardust.cc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class SlotColorConfigScreen extends Screen {

    private final Screen parent;
    private SlotColorConfig.Preset selectedPreset;
    private float customBrightness;
    private BrightnessSlider brightnessSlider;
    private ButtonWidget defaultBtn;
    private ButtonWidget darkBtn;
    private ButtonWidget customBtn;

    public SlotColorConfigScreen(Screen parent) {
        super(Text.literal("Creative Crafting Color Options"));
        this.parent = parent;
        SlotColorConfig cfg = SlotColorConfig.getInstance();
        this.selectedPreset = cfg.preset;
        this.customBrightness = cfg.customBrightness;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int btnY = 55;

        defaultBtn = ButtonWidget.builder(Text.literal("Default"), b -> selectPreset(SlotColorConfig.Preset.DEFAULT))
                .dimensions(cx - 155, btnY, 95, 20).build();
        darkBtn = ButtonWidget.builder(Text.literal("Dark"), b -> selectPreset(SlotColorConfig.Preset.DARK))
                .dimensions(cx - 50, btnY, 95, 20).build();
        customBtn = ButtonWidget.builder(Text.literal("Custom"), b -> selectPreset(SlotColorConfig.Preset.CUSTOM))
                .dimensions(cx + 55, btnY, 95, 20).build();

        this.addDrawableChild(defaultBtn);
        this.addDrawableChild(darkBtn);
        this.addDrawableChild(customBtn);

        brightnessSlider = new BrightnessSlider(cx - 100, btnY + 130, 200, 20, customBrightness);
        this.addDrawableChild(brightnessSlider);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save & Done"), b -> {
            applyAndSave();
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(cx - 75, this.height - 30, 150, 20).build());

        updateButtons();
        updateSlider();
    }

    private void selectPreset(SlotColorConfig.Preset preset) {
        this.selectedPreset = preset;
        updateButtons();
        updateSlider();
    }

    private void updateButtons() {
        defaultBtn.active = selectedPreset != SlotColorConfig.Preset.DEFAULT;
        darkBtn.active    = selectedPreset != SlotColorConfig.Preset.DARK;
        customBtn.active  = selectedPreset != SlotColorConfig.Preset.CUSTOM;
    }

    private void updateSlider() {
        brightnessSlider.visible = selectedPreset == SlotColorConfig.Preset.CUSTOM;
    }

    private void applyAndSave() {
        SlotColorConfig cfg = SlotColorConfig.getInstance();
        cfg.preset = selectedPreset;
        cfg.customBrightness = customBrightness;
        SlotColorConfig.save();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Use renderDarkening instead of the default blurred background to prevent
        // "Can only blur once per frame" crash when this screen is opened on top of
        // another screen that has already applied the blur this frame.
        this.renderDarkening(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 15, 0xFFFFFF);

        // --- Slot preview ---
        int previewY = 85;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Preview"), cx, previewY, 0xAAAAAA);

        SlotColorConfig tmp = new SlotColorConfig();
        tmp.preset = selectedPreset;
        tmp.customBrightness = customBrightness;
        int[] colors = tmp.getColors();
        int shadow  = colors[0];
        int hilight = colors[1];
        int fill    = colors[2];

        int slotSize = 18;
        int gap = 4;
        int totalW = 5 * slotSize + 4 * gap;
        int startX = cx - totalW / 2;
        int startY = previewY + 14;

        for (int i = 0; i < 5; i++) {
            int sx = startX + i * (slotSize + gap);
            int sy = startY;
            context.fill(sx,                sy,                sx + slotSize,     sy + 1,             shadow);  // top
            context.fill(sx,                sy + 1,            sx + 1,            sy + slotSize - 1,  shadow);  // left
            context.fill(sx + slotSize - 1, sy + 1,            sx + slotSize,     sy + slotSize - 1,  hilight); // right
            context.fill(sx,                sy + slotSize - 1, sx + slotSize,     sy + slotSize,      hilight); // bottom
            context.fill(sx + 1,            sy + 1,            sx + slotSize - 1, sy + slotSize - 1,  fill);    // interior
        }

        if (selectedPreset == SlotColorConfig.Preset.CUSTOM) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Brightness: " + Math.round(customBrightness * 100) + "%"),
                cx, startY + slotSize + 6, 0xFFFFAA);
        }
    }

    private class BrightnessSlider extends SliderWidget {
        BrightnessSlider(int x, int y, int width, int height, float initial) {
            super(x, y, width, height, Text.literal("Brightness: " + Math.round(initial * 100) + "%"), initial);
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Brightness: " + Math.round(this.value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            customBrightness = (float) this.value;
        }
    }
}
