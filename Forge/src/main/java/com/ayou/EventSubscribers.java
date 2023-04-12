package com.ayou;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import software.bernie.example.registry.BlockRegistry;

import java.util.Objects;

public class EventSubscribers {

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
        || event.getPacket() instanceof ServerboundContainerClosePacket) return;
        if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet){
            if (packet.getBlockState().getBlock() == Blocks.PLAYER_HEAD) event.setCanceled(true);
        }
        if (event.getPacket() instanceof ClientboundBlockEntityDataPacket entityDataPacket){
            if(!entityDataPacket.getType().equals(BlockEntityType.SKULL)) return;
            if (Minecraft.getInstance().level == null) return;
            SkullBlockEntity skullBlock = (SkullBlockEntity) Minecraft.getInstance().level.getBlockEntity(entityDataPacket.getPos());
            if (Objects.isNull(skullBlock) || Objects.isNull(skullBlock.getOwnerProfile())) return;
            if (skullBlock.getOwnerProfile().getName().startsWith("gecko")){
                Minecraft.getInstance().level.setBlockAndUpdate(skullBlock.getBlockPos(), BlockRegistry.FERTILIZER_BLOCK.get().defaultBlockState());
                skullBlock.setChanged();
            }
        }
//        Minecraft.getInstance().player.displayClientMessage(new TextComponent(event.getPacket().getClass().toGenericString()), false);
    }
}
