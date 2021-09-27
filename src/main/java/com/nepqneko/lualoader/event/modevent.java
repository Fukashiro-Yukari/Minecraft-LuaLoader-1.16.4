package com.nepqneko.lualoader.event;

import com.nepqneko.lualoader.lualoader;
import com.nepqneko.lualoader.networking.LuaReloadNetworking;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.luaj.lualoader.vm2.lib.jse.CoerceJavaToLua;

public class modevent {
    public modevent() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    @SubscribeEvent
    public void addHookEvent1(FMLClientSetupEvent e){
        ClientRegistry.registerKeyBinding(lualoader.OPEN_CONSOLE);

        lualoader.hookcall("OnClientSetup", CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent2(FMLCommonSetupEvent e){
        LuaReloadNetworking.register();

        lualoader.hookcall("OnSetup",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent3(FMLConstructModEvent e){
        lualoader.hookcall("OnConstructMod",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent4(FMLDedicatedServerSetupEvent e){
        lualoader.hookcall("OnServerSetup",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent5(FMLLoadCompleteEvent e){
        lualoader.hookcall("OnLoadComplete",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent6(FMLModIdMappingEvent e){
        lualoader.hookcall("OnModIdMapping",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent7(GatherDataEvent e){
        lualoader.hookcall("OnGatherData",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent8(InterModEnqueueEvent e){
        lualoader.hookcall("OnModEnqueue",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent9(InterModProcessEvent e){
        lualoader.hookcall("OnModProcess",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent10(ModLifecycleEvent e){
        lualoader.hookcall("OnModLifecycle",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent11(ParallelDispatchEvent e){
        lualoader.hookcall("OnParallelDispatch",CoerceJavaToLua.coerce(e));
    }
}
