package net.flytre.hplus.hopper;

import net.flytre.hplus.HopperUpgrade;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class UpgradeSlot extends Slot {
    private final int index;

    public UpgradeSlot(HopperPlusBlockEntity inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.index = index;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return (stack.getItem() instanceof HopperUpgrade && !((HopperPlusBlockEntity) inventory).hasUpgrade(stack));
    }

    public int getMaxItemCount() {
        return 1;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.inventory.setStack(this.index, stack);
        this.markDirty();
    }

    @Override
    public ItemStack getStack() {
        return this.inventory.getStack(this.index);
    }


}
