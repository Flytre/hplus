package net.flytre.hplus;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.flytre.hplus.filter.*;
import net.flytre.hplus.hopper.HopperPlusBlock;
import net.flytre.hplus.hopper.HopperPlusBlockEntity;
import net.flytre.hplus.hopper.HopperPlusScreen;
import net.flytre.hplus.hopper.HopperPlusScreenHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

public class RegistryHandler {


    public static final HopperPlusBlock HOPPER_PLUS = new HopperPlusBlock(FabricBlockSettings.of(Material.METAL).hardness(4.0f));
    public static final Item FILTER_UPGRADE = new FilterUpgrade(new FabricItemSettings().group(ItemGroup.REDSTONE));
    public static final Item SPEED_UPGRADE = new ItemUpgrade();
    public static final Item SPEED_UPGRADE_HIGH = new ItemUpgrade();
    public static final Item STACK_UPGRADE = new ItemUpgrade();
    public static final Item VACUUM_UPGRADE = new ItemUpgrade();
    public static final Item VOID_UPGRADE = new ItemUpgrade();
    public static final Item LOCK_UPGRADE = new ItemUpgrade();
    public static final Item BASE_UPGRADE = new Item(new FabricItemSettings().group(ItemGroup.REDSTONE));


    public static final ScreenHandlerType<FilterScreenHandler> FILTER_SCREEN_HANDLER  = ScreenHandlerRegistry.registerSimple(new Identifier("hplus", "upgrade_filter"), FilterScreenHandler::new);
    public static BlockEntityType<HopperPlusBlockEntity> HOPPER_PLUS_BLOCK_ENTITY;
    public static final ScreenHandlerType<HopperPlusScreenHandler> HOPPER_PLUS_SCREEN_HANDLER  = ScreenHandlerRegistry.registerSimple(new Identifier("hplus", "hopper_plus"), HopperPlusScreenHandler::new);


    public static final Identifier FILTER_MODE = new Identifier("hplus", "filter_mode");
    public static final Identifier FILTER_TRASH = new Identifier("hplus", "filter_trash");



    public static void onInit() {

        Registry.register(Registry.ITEM, new Identifier("hplus", "upgrade_base"), BASE_UPGRADE);
        Registry.register(Registry.ITEM, new Identifier("hplus", "upgrade_filter"), FILTER_UPGRADE);
        Registry.register(Registry.BLOCK, new Identifier("hplus", "hopper_plus"), HOPPER_PLUS);
        Registry.register(Registry.ITEM, new Identifier("hplus", "hopper_plus"), new BlockItem(HOPPER_PLUS, new Item.Settings().group(ItemGroup.REDSTONE)));
        HOPPER_PLUS_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, "hplus:hopper_plus", BlockEntityType.Builder.create(HopperPlusBlockEntity::new, HOPPER_PLUS).build(null));


        Registry.register(Registry.ITEM, new Identifier("hplus", "upgrade_vacuum"), VACUUM_UPGRADE);
        Registry.register(Registry.ITEM, new Identifier("hplus", "upgrade_speed"), SPEED_UPGRADE);
        Registry.register(Registry.ITEM, new Identifier("hplus", "upgrade_speed_high"), SPEED_UPGRADE_HIGH);
        Registry.register(Registry.ITEM, new Identifier("hplus", "upgrade_stack"), STACK_UPGRADE);
        Registry.register(Registry.ITEM, new Identifier("hplus", "upgrade_void"), VOID_UPGRADE);
        Registry.register(Registry.ITEM, new Identifier("hplus", "upgrade_lock"), LOCK_UPGRADE);


        ServerSidePacketRegistry.INSTANCE.register(FILTER_MODE, (packetContext, attachedData) -> {
            int state = attachedData.readInt();
            packetContext.getTaskQueue().execute(() -> {
                ItemStack stack = packetContext.getPlayer().getMainHandStack();
                if(stack.getTag() == null)
                    stack.setTag(new CompoundTag());
                stack.getTag().putInt("filterType",state);
            });
        });

    }

    @Environment(EnvType.CLIENT)
    public static void onInitClient() {
        ScreenRegistry.register(FILTER_SCREEN_HANDLER, FilterScreen::new);
        ScreenRegistry.register(HOPPER_PLUS_SCREEN_HANDLER, HopperPlusScreen::new);
    }


}
