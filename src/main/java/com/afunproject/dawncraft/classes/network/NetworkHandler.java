package com.afunproject.dawncraft.classes.network;

import com.afunproject.dawncraft.classes.Constants;
import net.minecraftforge.network.simple.SimpleChannel;
import net.smileycorp.atlas.api.network.NetworkUtils;

public class NetworkHandler {

    public static SimpleChannel NETWORK_INSTANCE;

    public static void initPackets() {
        NETWORK_INSTANCE = NetworkUtils.createChannel(Constants.loc("main"));
        NetworkUtils.registerMessage(NETWORK_INSTANCE,0, OpenClassGUIMessage.class);
        NetworkUtils.registerMessage(NETWORK_INSTANCE,1, PickClassMessage.class);
    }

}
