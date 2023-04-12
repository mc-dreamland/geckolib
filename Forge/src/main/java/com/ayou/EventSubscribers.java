package com.ayou;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
        || event.getPacket() instanceof ServerboundContainerClosePacket) return;
        if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet){
            System.out.println(Minecraft.getInstance().level.getBlockEntity(packet.getPos()));
            if (packet.getBlockState().getBlock() == Blocks.PLAYER_HEAD) event.setCanceled(true);
        }

        Minecraft.getInstance().player.displayClientMessage(new TextComponent(event.getPacket().getClass().toGenericString()), false);
    }
}
