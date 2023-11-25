package com.afunproject.dawncraft.classes;

import com.afunproject.dawncraft.classes.data.DCClassLoader;
import com.afunproject.dawncraft.classes.network.NetworkHandler;
import com.afunproject.dawncraft.classes.network.OpenClassGUIMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;

public class EventHandler {

    @SubscribeEvent
    public void addResourceReload(AddReloadListenerEvent event) {
        event.addListener(DCClassLoader.INSTANCE);
    }

    @SubscribeEvent
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PickedClass.class);
    }

    @SubscribeEvent
    public void attachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player &! (entity instanceof FakePlayer)) {
            event.addCapability(Constants.loc("picked_class"), new PickedClass.Provider());
        }
    }

    @SubscribeEvent
    public void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer)) return;
        LazyOptional<PickedClass> optional = player.getCapability(DCClasses.PICKED_CLASS);
        if (!optional.isPresent()) return;
        PickedClass cap = optional.orElseGet(null);
        if (!cap.hasPicked()) NetworkHandler.NETWORK_INSTANCE.sendTo(new OpenClassGUIMessage(),
                ((ServerPlayer) player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @SubscribeEvent
    public void joinedWorld(EntityJoinWorldEvent event) {
        Entity player = event.getEntity();
        if (!(player instanceof ServerPlayer)) return;
        LazyOptional<PickedClass> optional = player.getCapability(DCClasses.PICKED_CLASS);
        if (!optional.isPresent()) return;
        PickedClass cap = optional.orElseGet(null);
        if (!cap.hasEffect()) cap.applyEffect((Player) player);
    }

    @SubscribeEvent
    public void playerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player player = event.getPlayer();
        original.reviveCaps();
        LazyOptional<PickedClass> optionalOld = original.getCapability(DCClasses.PICKED_CLASS);
        LazyOptional<PickedClass> optional = player.getCapability(DCClasses.PICKED_CLASS);
        if (optionalOld.isPresent() && optional.isPresent()) optional.orElseGet(null).load(optionalOld.orElseGet(null).save());
    }

}
