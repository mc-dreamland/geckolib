package com.ayou;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import software.bernie.example.GeckoLibMod;
import software.bernie.example.registry.BlockRegistry;
import software.bernie.example.registry.ItemRegistry;

import java.util.Objects;

public class EventSubscribers {
    @SubscribeEvent
    public void onEntity(EntityEvent event){
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) return;
        if (event.getEntity() instanceof ItemEntity entity){
            if (entity.getItem().is(Items.PLAYER_HEAD)){
                if (entity.getItem().getTag() == null) return;
                if (GeckoLibMod.getSkullOwner(entity.getItem().getTag()).startsWith("gecko")){
                    entity.setItem(new ItemStack(ItemRegistry.FERTILIZER_ITEM.get()));
                }
            }
        }
    }

    @SubscribeEvent
    public void onJoinServer(PlayerEvent.PlayerLoggedInEvent event){
            event.getPlayer().getInventory().items.stream()
                    .filter(item->item.is(Items.PLAYER_HEAD))
                    .filter(ItemStack::hasTag)
                    .forEach(stack -> {
                        int index = event.getPlayer().getInventory().findSlotMatchingItem(stack);
                        String skullOwner = GeckoLibMod.getSkullOwner(stack.getTag());
                        if (skullOwner.startsWith("gecko")){
                            event.getPlayer().getInventory().items.set(index,new ItemStack(ItemRegistry.FERTILIZER_ITEM.get()));
                        }
                    });
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onBlockUpdate(PacketEvent event){
        // cancel right and left click
        if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet){
            if (packet.getBlockState().getBlock() == Blocks.PLAYER_HEAD) event.setCanceled(true);
        }
/*
        if (event.getPacket() instanceof ClientboundPlayerInfoPacket packet){
            if (Minecraft.getInstance().player == null) return;
            Minecraft.getInstance().player.getInventory().items.stream()
                    .filter(item->item.is(Items.PLAYER_HEAD))
                    .filter(ItemStack::hasTag)
                    .forEach(stack -> {
                        int index = Minecraft.getInstance().player.getInventory().findSlotMatchingItem(stack);
                        String skullOwner = GeckoLibMod.getSkullOwner(stack.getTag());
                        if (skullOwner.startsWith("gecko")){
                            Minecraft.getInstance().player.getInventory().items.set(index,new ItemStack(ItemRegistry.FERTILIZER_ITEM.get()));
                        }
                    });
        }
*/
        // place
        if (event.getPacket() instanceof ClientboundBlockEntityDataPacket entityDataPacket){
            if(!entityDataPacket.getType().equals(BlockEntityType.SKULL)) return;
            if (Minecraft.getInstance().level == null) return;
            BlockEntity block = Minecraft.getInstance().level.getBlockEntity(entityDataPacket.getPos());
            if (block instanceof SkullBlockEntity skullBlock){
                if (Objects.isNull(skullBlock.getOwnerProfile())) return;
                if (skullBlock.getOwnerProfile().getName().startsWith("gecko")){
                    Minecraft.getInstance().level.setBlockAndUpdate(skullBlock.getBlockPos(), BlockRegistry.FERTILIZER_BLOCK.get().defaultBlockState());
                    skullBlock.setChanged();
                }
            }
        }
        // pickup、click
        if (event.getPacket() instanceof ClientboundContainerSetSlotPacket slotPacket){
            if (slotPacket.getItem().is(Items.PLAYER_HEAD)){
                if (slotPacket.getItem().getTag() == null) return;
                if (GeckoLibMod.getSkullOwner(slotPacket.getItem().getTag()).startsWith("gecko")){
                    if (Minecraft.getInstance().player != null){
                        ClientboundContainerSetSlotPacket packet = new ClientboundContainerSetSlotPacket(
                                slotPacket.getContainerId(),slotPacket.getStateId(),slotPacket.getSlot(),new ItemStack(ItemRegistry.FERTILIZER_ITEM.get())
                        );
                        event.setPacket(packet);
                    }
                }
            }
        }
        // container
        if (event.getPacket() instanceof ClientboundContainerSetContentPacket contentPacket){
            for (int i = 0; i < contentPacket.getItems().size(); i++) {
                ItemStack item = contentPacket.getItems().get(i);
                if (item.is(Items.PLAYER_HEAD) && item.getTag() != null){
                    String skullOwner = GeckoLibMod.getSkullOwner(item.getTag());
                    if (skullOwner.startsWith("gecko")){
                        contentPacket.getItems().set(i,new ItemStack(ItemRegistry.FERTILIZER_ITEM.get()));
                    }
                }
            }
            NonNullList<ItemStack> itemStacks = NonNullList.withSize(contentPacket.getItems().size(), ItemStack.EMPTY);
            for (int i = 0; i < contentPacket.getItems().size(); ++i) {
                itemStacks.set(i,contentPacket.getItems().get(i).copy());
            }
            ClientboundContainerSetContentPacket packet = new ClientboundContainerSetContentPacket(
                    contentPacket.getContainerId(),contentPacket.getStateId(),itemStacks,contentPacket.getCarriedItem()
            );
            event.setPacket(packet);
        }
        // container click
        if (event.getPacket() instanceof ServerboundContainerClickPacket packet) {
            if (packet.getSlotNum() == -999) return;
            if (packet.getChangedSlots().isEmpty()) event.setCanceled(true);
        }
        if (event.getPacket() instanceof ClientboundAddEntityPacket entityPacket){
            BlockPos blockPos = new BlockPos(entityPacket.getX(),entityPacket.getY(),entityPacket.getZ());
            if (Minecraft.getInstance().level == null) return;
            LevelChunk chunk = Minecraft.getInstance().level.getChunkAt(blockPos);
            ChunkPos pos = chunk.getPos();
            AABB aabb = new AABB(new BlockPos(pos.getMinBlockX(), 0, pos.getMinBlockZ()),
                    new BlockPos(pos.getMaxBlockX(), 255, pos.getMaxBlockZ()));
            Minecraft.getInstance().level.getEntitiesOfClass(ItemEntity.class,aabb).forEach(entity -> {
                if (entity.getItem().is(Items.PLAYER_HEAD) && entity.getItem().getTag() != null){
                    String skullOwner = GeckoLibMod.getSkullOwner(entity.getItem().getTag());
                    if (skullOwner.startsWith("gecko")){
                        System.out.println("SkullID: "+entity.getId());
                        entity.setItem(new ItemStack(ItemRegistry.FERTILIZER_ITEM.get()));
                        System.out.println(Minecraft.getInstance().level.getEntity(entity.getId()));
                    }
                }
            });
        }
        if (event.getPacket() instanceof ClientboundSetEntityDataPacket packet) {
            if (Minecraft.getInstance().level == null) return;
            Entity entity = Minecraft.getInstance().level.getEntity(packet.getId());
        }
    }

    @SubscribeEvent
    public void packetDisplay(PacketEvent event) {
        //Displays every packet class in chat as the are received/sent
        if (event.getPacket() instanceof ServerboundMovePlayerPacket
            || event.getPacket() instanceof ClientboundMoveEntityPacket
        || event.getPacket() instanceof ClientboundSetTimePacket
        || event.getPacket() instanceof ServerboundKeepAlivePacket
        || event.getPacket() instanceof ClientboundRotateHeadPacket
        || event.getPacket() instanceof ClientboundTeleportEntityPacket
        || event.getPacket() instanceof ServerboundSetCreativeModeSlotPacket
        || event.getPacket() instanceof ClientboundSetEntityDataPacket
        || event.getPacket() instanceof ClientboundKeepAlivePacket
                || event.getPacket() instanceof ClientboundSetEntityMotionPacket
                || event.getPacket() instanceof ClientboundEntityEventPacket
                || event.getPacket() instanceof ClientboundSoundPacket
                || event.getPacket() instanceof ClientboundRemoveEntitiesPacket
                || event.getPacket() instanceof ClientboundUpdateAttributesPacket
                || event.getPacket() instanceof ServerboundSetCarriedItemPacket
                || event.getPacket() instanceof ClientboundBlockEventPacket
                || event.getPacket() instanceof ClientboundGameEventPacket
                || event.getPacket() instanceof ClientboundSetEquipmentPacket
                || event.getPacket() instanceof ClientboundBlockUpdatePacket
                || event.getPacket() instanceof ClientboundBlockEntityDataPacket
                || event.getPacket() instanceof ClientboundAddMobPacket
                || event.getPacket() instanceof ClientboundLevelChunkWithLightPacket
        || event.getPacket() instanceof ServerboundContainerClosePacket) return;
        Minecraft.getInstance().player.displayClientMessage(new TextComponent(event.getPacket().getClass().toGenericString()), false);
    }
}
