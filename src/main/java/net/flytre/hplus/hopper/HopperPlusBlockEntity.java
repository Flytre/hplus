package net.flytre.hplus.hopper;

import net.flytre.hplus.HopperUpgrade;
import net.flytre.hplus.RegistryHandler;
import net.flytre.hplus.filter.FilterInventory;
import net.flytre.hplus.filter.FilterUpgrade;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Tickable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HopperPlusBlockEntity extends LockableContainerBlockEntity implements Hopper, Tickable, SidedInventory {


    private final DefaultedList<ItemStack> upgrades;
    private DefaultedList<ItemStack> inventory;
    private int transferCooldown;
    private long lastTickTime;

    //Custom features
    private boolean stackUpgrade = false;
    private int maxCooldown = 8;
    private boolean vacuum = true;
    private boolean trash = false;
    private boolean locked = false;

    public HopperPlusBlockEntity() {
        super(RegistryHandler.HOPPER_PLUS_BLOCK_ENTITY);
        this.inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
        this.upgrades = DefaultedList.ofSize(9, ItemStack.EMPTY);
        this.transferCooldown = -1;
    }

    //Looks at an inventory and figures out which slots can be accessed from a certain side
    private static IntStream getAvailableSlots(Inventory inventory, Direction side) {
        return inventory instanceof SidedInventory ? IntStream.of(((SidedInventory) inventory).getAvailableSlots(side)) : IntStream.range(0, inventory.size());
    }

    //Checks if an inventory is totally empty
    private static boolean isInventoryEmpty(Inventory inv, Direction facing) {
        return getAvailableSlots(inv, facing).allMatch((i) -> inv.getStack(i).isEmpty());
    }

    //Looks at the inventory above the hopper. If it is not empty, pull from whatever slots are available in the down direction.
    private static boolean extract(HopperPlusBlockEntity hopper) {

        if(hopper.locked)
            return false;

        Inventory inventory = getInputInventory(hopper);

        if (inventory instanceof HopperPlusBlockEntity) {
            if (((HopperPlusBlockEntity) inventory).transferCooldown > 0)
                return false;
            if(willInsert((HopperPlusBlockEntity) inventory)) {
                return false;
            }
        }

        if (inventory != null) {
            Direction direction = hopper.getCachedState().get(HopperPlusBlock.FACING) == Direction.UP ? Direction.UP : Direction.DOWN;
            return !isInventoryEmpty(inventory, direction) && getAvailableSlots(inventory, direction).anyMatch((i) -> extract(hopper, inventory, i, direction));
        }

        if (hopper.vacuum) {
            Iterator<ItemEntity> var2 = getInputItemEntities(hopper).iterator();
            ItemEntity itemEntity;
            do {
                if (!var2.hasNext()) {
                    return false;
                }
                itemEntity = var2.next();
            } while (!extract(hopper, itemEntity));
            return true;
        } else
            return false;
    }

    //Pulls items from a particularly inventory's slot
    //Get the stack to extract, copy it, transfer the items, mark the inventory as modified.
    private static boolean extract(HopperPlusBlockEntity hopper, Inventory inventory, int slot, Direction side) {

        ItemStack slotStack = inventory.getStack(slot);

        if (!hopper.passFilterTest(slotStack))
            return false;

        if (!slotStack.isEmpty() && canExtract(inventory, slotStack, slot, side)) {
            ItemStack slotStackCopy = slotStack.copy();
            ItemStack stackTransfer = transfer(inventory, hopper, inventory.removeStack(slot, hopper.stackUpgrade ? inventory.getStack(slot).getCount() : 1), null);
            if (stackTransfer.isEmpty()) {

                if(inventory instanceof HopperPlusBlockEntity)
                    ((HopperPlusBlockEntity)inventory).transferCooldown = ((HopperPlusBlockEntity) inventory).maxCooldown;

                inventory.markDirty();
                return true;
            }
            inventory.setStack(slot, slotStackCopy);
        }
        return false;
    }

    //Transfers items between two inventories.
    private static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, @Nullable Direction side) {
        if (to instanceof SidedInventory && side != null) {
            SidedInventory sidedInventory = (SidedInventory) to;
            int[] is = sidedInventory.getAvailableSlots(side);
            for (int i = 0; i < is.length && !stack.isEmpty(); ++i) {
                stack = transfer(from, to, stack, is[i], side);
            }
        } else {
            int j = to.size();

            for (int k = 0; k < j && !stack.isEmpty(); ++k) {
                stack = transfer(from, to, stack, k, side);
            }
        }

        return stack;
    }

    //Check if the side is valid and the item is accepted
    private static boolean canInsert(Inventory inventory, ItemStack stack, int slot, @Nullable Direction side) {
        if (!inventory.isValid(slot, stack)) {
            return false;
        } else {
            return !(inventory instanceof SidedInventory) || ((SidedInventory) inventory).canInsert(slot, stack, side);
        }
    }

    //Check if the side is extractable
    private static boolean canExtract(Inventory inv, ItemStack stack, int slot, Direction facing) {
        return !(inv instanceof SidedInventory) || ((SidedInventory) inv).canExtract(slot, stack, facing);
    }

    //Transfer items from a specific slot to another inventory
    private static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, int slot, @Nullable Direction direction) {
        ItemStack itemStack = to.getStack(slot);
        if (canInsert(to, stack, slot, direction)) {
            boolean destinationEmpty = false;
            boolean inventoryEmpty = to.isEmpty();
            if (itemStack.isEmpty()) {
                to.setStack(slot, stack);
                stack = ItemStack.EMPTY;
                destinationEmpty = true;
            } else if (canMergeItems(itemStack, stack)) {
                int i = stack.getMaxCount() - itemStack.getCount();
                int j = Math.min(stack.getCount(), i);
                stack.decrement(j);
                itemStack.increment(j);
                destinationEmpty = j > 0;
            }

            if (destinationEmpty) {
                if (inventoryEmpty && to instanceof HopperPlusBlockEntity) {
                    HopperPlusBlockEntity hopperBlockEntity = (HopperPlusBlockEntity) to;
                    if (!hopperBlockEntity.isDisabled()) {
                        int k = 0;
                        if (from instanceof HopperPlusBlockEntity) {
                            HopperPlusBlockEntity hopperBlockEntity2 = (HopperPlusBlockEntity) from;
                            if (hopperBlockEntity.lastTickTime >= hopperBlockEntity2.lastTickTime) {
                                k = 1;
                            }
                        }

                        hopperBlockEntity.setCooldown(hopperBlockEntity.maxCooldown - k);
                    }
                }

                to.markDirty();
            }
        }

        return stack;
    }

    @Nullable
    //Gets the inventory above the hopper
    private static Inventory getInputInventory(Hopper hopper) {
        double yDiff = 1.0;
        if (hopper instanceof HopperPlusBlockEntity) {
            yDiff = ((HopperPlusBlockEntity) hopper).getCachedState().get(HopperPlusBlock.FACING) == Direction.UP ? -1.0 : 1.0;
        }
        return getInventoryAt(hopper.getWorld(), hopper.getHopperX(), hopper.getHopperY() + yDiff, hopper.getHopperZ());
    }

    @Nullable
    private static Inventory getInventoryAt(World world, BlockPos blockPos) {
        return getInventoryAt(world, (double) blockPos.getX() + 0.5D, (double) blockPos.getY() + 0.5D, (double) blockPos.getZ() + 0.5D);
    }

    @Nullable
    private static Inventory getInventoryAt(World world, double x, double y, double z) {
        Inventory inventory = null;
        BlockPos blockPos = new BlockPos(x, y, z);
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof InventoryProvider) {
            inventory = ((InventoryProvider) block).getInventory(blockState, world, blockPos);
        } else if (block.hasBlockEntity()) {
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof Inventory) {
                inventory = (Inventory) blockEntity;
                if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock) {
                    inventory = ChestBlock.getInventory((ChestBlock) block, blockState, world, blockPos, true);
                }
            }
        }

        if (inventory == null) {
            List<Entity> list = world.getOtherEntities(null, new Box(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D), EntityPredicates.VALID_INVENTORIES);
            if (!list.isEmpty()) {
                inventory = (Inventory) list.get(world.random.nextInt(list.size()));
            }
        }

        return inventory;
    }

    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        if (first.getItem() != second.getItem()) {
            return false;
        } else if (first.getDamage() != second.getDamage()) {
            return false;
        } else if (first.getCount() > first.getMaxCount()) {
            return false;
        } else {
            return ItemStack.areTagsEqual(first, second);
        }
    }

    //Vacuum upgrade
    public static List<ItemEntity> getInputItemEntities(Hopper hopper) {
        return hopper.getInputAreaShape().getBoundingBoxes().stream().flatMap((box) ->
                hopper.getWorld().getEntitiesByClass(ItemEntity.class, box.offset(hopper.getHopperX() - 0.5D, hopper.getHopperY() - 0.5D, hopper.getHopperZ() - 0.5D), EntityPredicates.VALID_ENTITY).stream()).collect(Collectors.toList());
    }

    public static boolean extract(HopperPlusBlockEntity inventory, ItemEntity itemEntity) {
        boolean bl = false;
        ItemStack itemStack = itemEntity.getStack().copy();

        if (!inventory.passFilterTest(itemStack))
            return false;

        ItemStack itemStack2 = transfer(null, inventory, itemStack, null);
        if (itemStack2.isEmpty()) {
            bl = true;
            itemEntity.remove();
        } else {
            itemEntity.setStack(itemStack2);
        }

        return bl;
    }

    public void onEntityCollided(Entity entity) {

        if (!vacuum || locked)
            return;

        if (entity instanceof ItemEntity) {
            BlockPos blockPos = this.getPos();
            if (VoxelShapes.matchesAnywhere(VoxelShapes.cuboid(entity.getBoundingBox().offset(-blockPos.getX(), -blockPos.getY(), -blockPos.getZ())), this.getInputAreaShape(), BooleanBiFunction.AND)) {
                this.insertAndExtract(() -> extract(this, (ItemEntity) entity));
            }
        }

    }

    public int getFirstEmptyUpgradeSlot() {
        for (int i = 0; i < getUpgrades().size(); i++)
            if (getUpgrades().get(i) == ItemStack.EMPTY)
                return i;
        return -1;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot > 4 || locked)
            return false;
        return passFilterTest(stack);
    }

    public DefaultedList<ItemStack> getUpgrades() {
        return this.upgrades;
    }

    @Override
    protected Text getContainerName() {
        return new TranslatableText("container.hplus.hopper_plus");
    }

    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.fromTag(tag, this.inventory);
        Tag items = tag.get("Items");
        Tag upgrades = tag.get("Upgrades");
        tag.put("Items", upgrades);
        Inventories.fromTag(tag, this.upgrades);
        tag.put("Items", items);
        this.transferCooldown = tag.getInt("TransferCooldown");
        onUpgradesUpdated();
    }

    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        Inventories.toTag(tag, this.upgrades);
        Tag upgrades = tag.get("Items");
        tag.put("Upgrades", upgrades);
        tag.remove("Items");
        Inventories.toTag(tag, this.inventory);
        tag.putInt("TransferCooldown", this.transferCooldown);
        return tag;
    }

    public int size() {
        return this.inventory.size();
    }

    public Set<Item> getFilterItems() {
        ItemStack f = null;
        for (ItemStack i : getUpgrades())
            if (i.getItem() instanceof FilterUpgrade)
                f = i;
        if (f == null)
            return null;
        FilterInventory inv = FilterUpgrade.getInventory(f);
        return inv.getFilterItems();
    }

    public boolean passFilterTest(ItemStack stack) {
        ItemStack f = null;
        for (ItemStack i : getUpgrades())
            if (i.getItem() instanceof FilterUpgrade)
                f = i;
        if (f == null)
            return true;
        FilterInventory inv = FilterUpgrade.getInventory(f);
        return inv.passFilterTest(stack);
    }

    public boolean hasUpgrade(ItemStack upgrade) {

        if (!(upgrade.getItem() instanceof HopperUpgrade))
            return false;

        for (ItemStack stack : upgrades) {
            if (stack.getItem() == upgrade.getItem())
                return true;
        }
        return false;
    }


    public void onUpgradesUpdated() {
        this.stackUpgrade = hasUpgrade(new ItemStack(RegistryHandler.STACK_UPGRADE));
        this.vacuum = hasUpgrade(new ItemStack(RegistryHandler.VACUUM_UPGRADE));
        this.trash = hasUpgrade(new ItemStack(RegistryHandler.VOID_UPGRADE));
        this.maxCooldown = 8;
        if (hasUpgrade(new ItemStack(RegistryHandler.SPEED_UPGRADE)))
            this.maxCooldown = 4;
        if (hasUpgrade(new ItemStack(RegistryHandler.SPEED_UPGRADE_HIGH)))
            this.maxCooldown = 2;
        this.locked = hasUpgrade(new ItemStack(RegistryHandler.LOCK_UPGRADE));
    }

    @Override
    public boolean isEmpty() {
        return this.getInvStackList().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {

        if (slot > 4)
            return this.getUpgrades().get(slot - 5);
        return this.getInvStackList().get(slot);
    }

    public ItemStack removeStack(int slot, int amount) {

        return slot < 5 ? Inventories.splitStack(this.getInvStackList(), slot, amount) : Inventories.splitStack(this.getUpgrades(), slot - 5, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return slot < 5 ?
                Inventories.removeStack(this.getInvStackList(), slot) :
                Inventories.removeStack(this.getUpgrades(), slot);
    }

    public void setStack(int slot, ItemStack stack) {
        if (slot > 4) {
            this.getUpgrades().set(slot - 5, stack);
            return;
        }
        this.getInvStackList().set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }

    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (this.world.getBlockEntity(this.pos) != this)
            return false;
        else
            return player.squaredDistanceTo((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;

    }

    public void tick() {

        if (this.world != null && !this.world.isClient) {
            --this.transferCooldown;
            if (this.transferCooldown > -1)
                this.lastTickTime = this.world.getTime();
            if (!this.needsCooldown()) {
                this.setCooldown(0);
                this.insertAndExtract(() -> extract(this));
            }

        }
    }

    public void onClose(PlayerEntity player) {
        this.onUpgradesUpdated();
    }

    private void insertAndExtract(Supplier<Boolean> extractMethod) {
        if (this.world != null && !this.world.isClient) {
            if (!this.needsCooldown() && this.getCachedState().get(HopperPlusBlock.ENABLED) && !locked) {
                boolean bl = false;

                if (!isEmpty() && trash)
                    getInvStackList().clear();

                if (!this.isFull()) {
                    bl = extractMethod.get();
                }


                if (!this.isEmpty()) {
                    bl |= this.insert();
                }

                if (bl) {
                    this.setCooldown(maxCooldown);
                    this.markDirty();
                }
            }

        }
    }

    private boolean isFull() {
        Iterator<ItemStack> inventoryIterator = this.inventory.iterator();

        ItemStack itemStack;
        do {
            if (!inventoryIterator.hasNext()) {
                return true;
            }

            itemStack = inventoryIterator.next();
        } while (!itemStack.isEmpty() && itemStack.getCount() == itemStack.getMaxCount());

        return false;
    }


    private static boolean willInsert(HopperPlusBlockEntity hopper) {
        Inventory inventory = hopper.getOutputInventory();
        if (inventory == null)
            return false;
        return hopper.insert();
    }

    //insert items to an inventory from the hopper
    private boolean insert() {
        Inventory inventory = this.getOutputInventory();
        if (inventory == null)
            return false;

        if(transferCooldown > 0 || locked)
            return false;

        if(inventory instanceof HopperPlusBlockEntity && willInsert((HopperPlusBlockEntity) inventory))
            return false;

        if (world instanceof ServerWorld) {
            BlockState toInsertTo = world.getBlockState(this.pos.offset(this.getCachedState().get(HopperPlusBlock.FACING)));
            if (toInsertTo.getBlock() == Blocks.COMPOSTER)
                return fillComposter();
        }

        Direction direction = this.getCachedState().get(HopperPlusBlock.FACING).getOpposite();
        if (!this.isInventoryFull(inventory, direction)) {
            for (int i = 0; i < this.size(); ++i) {
                if (!this.getStack(i).isEmpty()) {
                    ItemStack itemStack = this.getStack(i).copy();
                    ItemStack removed = this.removeStack(i, stackUpgrade ? this.getStack(i).getCount() : 1);
                    ItemStack itemStack2 = transfer(this, inventory, removed, direction);
                    if (stackUpgrade && itemStack2.getCount() < itemStack.getCount()) {
                        inventory.markDirty();
                        this.setStack(i, itemStack2);
                        return true;
                    } else if(!stackUpgrade && itemStack2.isEmpty()) {
                        inventory.markDirty();
                        return true;
                    } else {
                        this.setStack(i,itemStack);
                    }
                }
            }

        }
        return false;
    }

    private boolean fillComposter() {
        Direction direction = this.getCachedState().get(HopperPlusBlock.FACING);
        assert world != null;
        BlockState state = world.getBlockState(this.pos.offset(direction));
        Block block = state.getBlock();
        if (block != Blocks.COMPOSTER)
            return false;
        for (int i = 0; i < this.size(); ++i) {
            if (!this.getStack(i).isEmpty()) {
                if (ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(getStack(i).getItem())) {
                    ComposterBlock.compost(state, (ServerWorld) world, getStack(i), this.pos.offset(direction));
                }

            }
        }
        return false;

    }

    private boolean isInventoryFull(Inventory inv, Direction direction) {
        return getAvailableSlots(inv, direction).allMatch((i) -> {
            ItemStack itemStack = inv.getStack(i);
            return itemStack.getCount() >= itemStack.getMaxCount();
        });
    }

    @Nullable
    private Inventory getOutputInventory() {
        Direction direction = this.getCachedState().get(HopperPlusBlock.FACING);
        return getInventoryAt(this.getWorld(), this.pos.offset(direction));
    }

    public double getHopperX() {
        return (double) this.pos.getX() + 0.5D;
    }

    public double getHopperY() {
        return (double) this.pos.getY() + 0.5D;
    }

    public double getHopperZ() {
        return (double) this.pos.getZ() + 0.5D;
    }

    private void setCooldown(int cooldown) {
        this.transferCooldown = cooldown;
    }

    private boolean needsCooldown() {
        return this.transferCooldown > 0;
    }

    private boolean isDisabled() {
        return this.transferCooldown > maxCooldown;
    }

    protected DefaultedList<ItemStack> getInvStackList() {
        return this.inventory;
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new HopperPlusScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new HopperPlusScreenHandler(syncId, playerInventory, this);
    }


    @Override
    public void clear() {
        this.getInvStackList().clear();
        this.upgrades.clear();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{0,1,2,3,4};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return !locked && transferCooldown <= 0;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return !locked && transferCooldown <= 0;
    }
}
