package net.flytre.hplus.filter;

import net.flytre.hplus.RegistryHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashSet;

public class FilterScreenHandler extends ScreenHandler {
    private final FilterInventory inventory;
    private final PlayerInventory playerInventory;
    private final int inventoryHeight = 3;


    public FilterScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new FilterInventory(new CompoundTag(),0));
    }

    public FilterScreenHandler(final int syncId, final PlayerInventory playerInventory, final Inventory inventory) {
        super(RegistryHandler.FILTER_SCREEN_HANDLER, syncId);
        this.inventory = (FilterInventory) inventory;
        this.playerInventory = playerInventory;
        checkSize(inventory, 9 * inventoryHeight);
        inventory.onOpen(playerInventory.player);
        setupSlots(false);
    }

    public FilterInventory getInventory() {
        return inventory;
    }

    public void setupSlots(final boolean includeChestInventory) {
        int i = (this.inventoryHeight - 4) * 18;

        int n;
        int m;
        for (n = 0; n < this.inventoryHeight; ++n) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new Slot(inventory, m + n * 9, 8 + m * 18, 18 + n * 18));
            }
        }

        for (n = 0; n < 3; ++n) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 102 + n * 18 + i));
            }
        }

        for (n = 0; n < 9; ++n) {
            this.addSlot(new Slot(playerInventory, n, 8 + n * 18, 161 + i));
        }
    }

    @Override
    public ItemStack onSlotClick(int slotId, int clickData, SlotActionType actionType, PlayerEntity playerEntity) {
        if (slotId >= 0) {
            ItemStack stack = getSlot(slotId).getStack();
            boolean isPlayerInventory = slotId >= inventory.size();
            if(!isPlayerInventory) {
                inventory.removeStack(slotId);
            } else {
                HashSet<Item> items = new HashSet<>();
                items.add(stack.getItem());
                if(!inventory.containsAny(items))
                    inventory.put(stack.getItem());
                else
                    return stack;
            }

            getSlot(slotId).inventory.markDirty();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void close(final PlayerEntity player) {
        super.close(player);
        inventory.onClose(player);
    }

    @Override
    public boolean canUse(final PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack transferSlot(final PlayerEntity player, final int invSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        return false;
    }

}