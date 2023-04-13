/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package software.bernie.example;

import com.ayou.ChannelHandlerInput;
import com.ayou.EventSubscribers;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.example.registry.BlockRegistry;
import software.bernie.example.registry.EntityRegistry;
import software.bernie.example.registry.ItemRegistry;
import software.bernie.example.registry.SoundRegistry;
import software.bernie.example.registry.TileRegistry;
import software.bernie.geckolib3.GeckoLib;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.controller.AnimationController.ModelFetcher;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;

import java.util.*;

@EventBusSubscriber
@Mod(GeckoLib.ModID)
public class GeckoLibMod {
    public static HashMap<BlockPos,String> map = new HashMap<>();
    public static CreativeModeTab geckolibItemGroup;

    /**
     * When set to true, prevents examples from being registered.
     *
     * @deprecated due to mod loading order, setting this in your mod may not have an effect.
     * Use the {@link #DISABLE_EXAMPLES_PROPERTY_KEY system property} instead.
     */
    @Deprecated(since = "3.0.40")

    public GeckoLibMod() {
        GeckoLib.initialize();
        if (shouldRegisterExamples()) {
            // 初始化实、物品、方块
            IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
            EntityRegistry.ENTITIES.register(bus);
            ItemRegistry.ITEMS.register(bus);
            TileRegistry.TILES.register(bus);
            BlockRegistry.BLOCKS.register(bus);
            SoundRegistry.SOUNDS.register(bus);
            geckolibItemGroup = new CreativeModeTab(CreativeModeTab.getGroupCountSafe(), "geckolib_examples") {
                @Override
                public ItemStack makeIcon() {
                    return new ItemStack(ItemRegistry.JACK_IN_THE_BOX.get());
                }
            };
        }
        MinecraftForge.EVENT_BUS.register(new ChannelHandlerInput());
        MinecraftForge.EVENT_BUS.register(new EventSubscribers());
    }

    public static void replaceTile(LevelChunk chunk){
        if (Minecraft.getInstance().level == null) return;
        List<SkullBlockEntity> collect = chunk.getBlockEntities().values().stream().filter(entity -> entity instanceof SkullBlockEntity)
                .map(entity -> (SkullBlockEntity) entity).toList();
        if (collect.size() == 0) return;
        collect.stream().filter(skullBlock -> Objects.nonNull(skullBlock.getOwnerProfile()))
                .forEach(skullBlock -> {
                    if (skullBlock.getOwnerProfile().getName().startsWith("geckolib:")){
                        BlockPos blockPos = skullBlock.getBlockPos();
                        Minecraft.getInstance().level.setBlockAndUpdate(blockPos,BlockRegistry.FERTILIZER_BLOCK.get().defaultBlockState());
                        skullBlock.setChanged();
                        map.put(blockPos,skullBlock.getOwnerProfile().getName());
                    }
                });
    }


    // DROP Server Only
    @SubscribeEvent
    public static void onDrop(ItemTossEvent event){
    }

    public static String getSkullOwner(CompoundTag compoundtag){
        String skullOwner = "";
        if (compoundtag.contains("SkullOwner", 8)) {
            skullOwner = compoundtag.getString("SkullOwner");
        }else if (compoundtag.contains("SkullOwner", 10)) {
            CompoundTag compoundtag1 = compoundtag.getCompound("SkullOwner");
            if (compoundtag1.contains("Name", 8)) {
                skullOwner = compoundtag1.getString("Name");
            }
        }
        return skullOwner;
    }

    @SubscribeEvent
    public static void onItem(TickEvent.PlayerTickEvent event){
        if (!event.player.level.isClientSide) return;
//        for (int i = 0; i < event.player.containerMenu.getItems().size(); i++) {
//            ItemStack item = event.player.containerMenu.getItems().get(i);
//            if (item.is(Items.PLAYER_HEAD)){
//                if (item.getTag() != null){
//                    CompoundTag compoundtag = item.getTag();
//                    if (getSkullOwner(compoundtag).startsWith("geckolib:")){
//                        event.player.getInventory().setItem(i, new ItemStack(BlockRegistry.FERTILIZER_BLOCK.get().asItem()));
//                    }
//                }
//            }
//        }
//        event.player.getInventory().items.stream().filter(stack-> stack.is(Items.PLAYER_HEAD))
//                .forEach(System.out::println);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.Load event){
        if (Minecraft.getInstance().level == null) return;
        LevelChunk chunk = Minecraft.getInstance().level.getChunkAt(new BlockPos(event.getChunk().getPos().x << 4, 255, event.getChunk().getPos().z << 4));
        replaceTile(chunk);
    }

    @SubscribeEvent
    public static void onEntityRemoved(EntityLeaveWorldEvent event) {
        if (event.getEntity() == null) {
            return;
        }
        if (event.getEntity().getUUID() == null) {
            return;
        }
        if (event.getWorld().isClientSide)
            GeoArmorRenderer.LIVING_ENTITY_RENDERERS.values().forEach(instances -> {
                if (instances.containsKey(event.getEntity().getUUID())) {
                    ModelFetcher<?> beGone = instances.get(event.getEntity().getUUID());
                    AnimationController.removeModelFetcher(beGone);
                    instances.remove(event.getEntity().getUUID());
                }
            });
    }

    /**
     * Returns whether examples are to be registered. Examples are registered when:
     * <ul>
     *     <li>The mod is running in a development environment; <em>and</em></li>
     *     <li>{@link #DISABLE_IN_DEV} is not set to true; <em>and</em></li>
     *     <li>the system property defined by {@link #DISABLE_EXAMPLES_PROPERTY_KEY} is not set to "true".</li>
     * </ul>
     *
     * @return whether the examples are to be registered
     */
    static boolean shouldRegisterExamples() {
        return true;
    }
}
