package net.flytre.hplus.filter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collections;

public class FilterButton extends ButtonWidget {

    private final int textureWidth;
    private final int textureHeight;
    private final Identifier texture;
    private Text tooltip;
    private ButtonTooltipRenderer tooltipRenderer;


    public FilterButton(int x, int y, int width, int height, Identifier texture, ButtonWidget.PressAction onPress) {
        super(x, y, width, height, LiteralText.EMPTY, onPress);
        this.textureHeight = height * 2;
        this.textureWidth = width;
        this.texture = texture;
    }

    public void setTooltips(Text frame1) {
        tooltip = frame1;
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
            getTooltipRenderer().draw(matrices, Collections.singletonList(tooltip),mouseX,mouseY);
    }


    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (MinecraftClient.getInstance() == null)
            return;

        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getTextureManager().bindTexture(texture);
        this.hovered = isHovering(mouseX, mouseY);
        int y = hovered ? this.height : 0;
        drawTexture(matrices, x, this.y, 0, y, width, height, textureWidth, textureHeight);

        if (isHovering(mouseX, mouseY))
            renderToolTip(matrices, mouseX, mouseY);
    }
}
