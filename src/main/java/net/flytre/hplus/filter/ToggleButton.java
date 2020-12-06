package net.flytre.hplus.filter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Collections;

public class ToggleButton extends ButtonWidget {

    private final int textureWidth;
    private final int textureHeight;
    private final Identifier texture;
    private int frame;
    private Text tooltip;
    private Text tooltip2;
    private ButtonTooltipRenderer tooltipRenderer;


    public ToggleButton(int x, int y, int width, int height, int frame, Identifier texture, PressAction onPress) {
        super(x, y, width, height, LiteralText.EMPTY, onPress);
        this.textureHeight = height * 4;
        this.textureWidth = width;
        this.texture = texture;
        this.frame = frame;
    }

    public void setTooltips(Text frame1, Text frame2) {
        tooltip = frame1;
        tooltip2 = frame2;
    }

    public ButtonTooltipRenderer getTooltipRenderer() {
        return tooltipRenderer;
    }

    public void setTooltipRenderer(ButtonTooltipRenderer tooltipRenderer) {
        this.tooltipRenderer = tooltipRenderer;
    }

    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
    }

    public void renderToolTip(MatrixStack matrices, int mouseX, int mouseY) {

        if (!(frame >= 2))
            getTooltipRenderer().draw(matrices, Collections.singletonList(tooltip),mouseX,mouseY);
        else
            getTooltipRenderer().draw(matrices, Collections.singletonList(tooltip2),mouseX,mouseY);

    }


    public void otherFrame() {
        this.frame += 2;
        this.frame %= 4;
    }

    public int getStateNumber() {
        return frame < 2 ? 0 : 1;
    }

    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (MinecraftClient.getInstance() == null)
            return;

        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getTextureManager().bindTexture(texture);
        this.hovered = isHovering(mouseX, mouseY);
        int y = this.height * frame;
        if (hovered)
            y += this.height;

        drawTexture(matrices, x, this.y, 0, y, width, height, textureWidth, textureHeight);

        if (isHovering(mouseX, mouseY))
            renderToolTip(matrices, mouseX, mouseY);
    }


}

