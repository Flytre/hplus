package net.flytre.hplus.hopper;

import net.flytre.hplus.HopperUpgrade;
import net.flytre.hplus.RegistryHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;


public class HopperPlusScreenHandler extends ScreenHandler {

    private final HopperPlusBlockEntity hopperInventory;
    private final PlayerInventory playerInventory;

    public HopperPlusScreenHandler(int syncID, PlayerInventory playerInventory) {
        this(syncID, playerInventory, new HopperPlusBlockEntity());


    }

    HopperPlusScreenHandler(int syncID, PlayerInventory playerInventory, HopperPlusBlockEntity inventory) {
        super(RegistryHandler.HOPPER_PLUS_SCREEN_HANDLER, syncID);
        this.playerInventory = playerInventory;
        this.hopperInventory = inventory;
        hopperInventory.onOpen(playerInventory.player);
        //Add slots
        //hopper inventory
        int l;
        for (l = 0; l < 5; ++l) {
            this.addSlot(new Slot(hopperInventory, l, 44 + l * 18, 41));
        }

        //filter inventory
        for (l = 0; l < 9; ++l) {
            this.addSlot(new UpgradeSlot(hopperInventory, 5 + l, 8 + l * 18, 14));
        }

        //player inventory
        for (l = 0; l < 3; ++l) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInventory, k + l * 9 + 9, 8 + k * 18, 67 + l * 18));
            }
        }

        for (l = 0; l < 9; ++l) {
            this.addSlot(new Slot(playerInventory, l, 8 + l * 18, 125));
        }
    }

    public boolean hasUpgrade(ItemStack g) {
        return hopperInventory.hasUpgrade(g);
    }

    @Override
    public ItemStack onSlotClick(int slotId, int clickData, SlotActionType actionType, PlayerEntity playerEntity) {
        if (slotId > 0) {
            ItemStack stack = getSlot(slotId).getStack();
            int upgradeSlot = hopperInventory.getFirstEmptyUpgradeSlot();
            if (stack.getItem() instanceof HopperUpgrade && actionType == SlotActionType.QUICK_MOVE) {
                if (slotId >= 5 && slotId <= 13) {
                    if(playerInventory.insertStack(stack)) {
                        getSlot(slotId).setStack(ItemStack.EMPTY);
                        hopperInventory.onUpgradesUpdated();
                        return ItemStack.EMPTY;
                    }
                } else if(upgradeSlot != -1 && !hasUpgrade(stack)) {
                    ItemStack stack2 = stack.copy();
                    stack2.setCount(1);
                    stack.decrement(1);
                    hopperInventory.getUpgrades().set(upgradeSlot, stack2);
                    getSlot(slotId).setStack(stack);
                    hopperInventory.onUpgradesUpdated();
                    return ItemStack.EMPTY;
                }
            }
        }
        ItemStack stack = super.onSlotClick(slotId, clickData, actionType, playerEntity);
        hopperInventory.onUpgradesUpdated();
        return stack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.hopperInventory.canPlayerUse(player);
    }

    public ItemStack transferSlot(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack itemStack2 = slot.getStack();
            itemStack = itemStack2.copy();
            if (index < this.hopperInventory.size()) {
                if (!this.insertItem(itemStack2, 14, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(itemStack2, 0, 5, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return itemStack;
    }

    public void close(PlayerEntity player) {
        super.close(player);
        this.hopperInventory.onClose(player);
    }
}
