package com.nepqneko.lualoader.networking;

import com.nepqneko.lualoader.lualoader;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class LuaReloadSendPack {
    public LuaReloadSendPack(){}

    public void handler(Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(lualoader::AutoReloadLua);
        ctx.get().setPacketHandled(true);
    }
}
