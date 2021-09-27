package com.nepqneko.lualoader.event;

import com.nepqneko.lualoader.lualoader;
import com.nepqneko.lualoader.resource.luaresourcepackfinder;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import org.luaj.lualoader.vm2.lib.jse.CoerceJavaToLua;

public class forgeevent {
    public forgeevent(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void addHookEvent12(FMLServerAboutToStartEvent e){
        lualoader.server = e.getServer();

        e.getServer().getResourcePacks().addPackFinder(luaresourcepackfinder.DATA);

        lualoader.hookcall("OnServerAboutToStart", CoerceJavaToLua.coerce(e));
    }
}
