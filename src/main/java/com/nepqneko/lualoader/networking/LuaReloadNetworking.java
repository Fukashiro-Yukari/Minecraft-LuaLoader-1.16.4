package com.nepqneko.lualoader.networking;

import com.nepqneko.lualoader.lualoader;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class LuaReloadNetworking {
    public static SimpleChannel INSTANCE;
    private static int id = 0;

    public static int getid(){
        return id++;
    }

    public static void register(){
        INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(lualoader.MOD_ID,"server_reload_lua"),() -> "1.0",(v) -> true,(v) -> true);
        INSTANCE.registerMessage(getid(),LuaReloadSendPack.class,(pack,buffer) -> {},(buffer) -> new LuaReloadSendPack(),LuaReloadSendPack::handler);
    }
}
