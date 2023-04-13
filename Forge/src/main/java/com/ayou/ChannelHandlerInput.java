package com.ayou;

import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ChannelHandlerInput {
    public static boolean firstConnection = true;

    @SubscribeEvent
    public void init(ClientPlayerNetworkEvent.LoggedInEvent event)  {

        if(firstConnection) {

            firstConnection = false;
            ChannelPipeline pipeline = event.getConnection().channel().pipeline();

            pipeline.addBefore("packet_handler","listener", new PacketListener());

        }
    }

    @SubscribeEvent (priority = EventPriority.HIGHEST)
    public void onDisconnect(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        firstConnection = true;
    }
}
