package net.flytre.hplus.filter;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

@FunctionalInterface
public interface ButtonTooltipRenderer {

    void draw(MatrixStack matrices, List<Text> lines, int x, int y);
}
