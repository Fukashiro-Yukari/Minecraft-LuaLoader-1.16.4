package com.nepqneko.lualoader.lualib;

import com.nepqneko.lualoader.item.luaitemgroup;
import com.nepqneko.lualoader.lualoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.luaj.lualoader.vm2.LuaTable;
import org.luaj.lualoader.vm2.LuaValue;
import org.luaj.lualoader.vm2.lib.OneArgFunction;
import org.luaj.lualoader.vm2.lib.TwoArgFunction;
import org.luaj.lualoader.vm2.lib.jse.CoerceJavaToLua;

import java.util.Map;

public class MAPI extends TwoArgFunction {
    public LuaValue call(LuaValue modname, LuaValue env){
        LuaTable t = new LuaTable();

        t.set("Msg",new Msg());
        t.set("IsDedicatedServer",new IsDedicatedServer());
        t.set("IsClient",new IsClient());
        t.set("GetLuaItem",new GetLuaItem());
        t.set("GetLuaItemGroup",new GetLuaItemGroup());
        t.set("GetMinecraft",new GetMinecraftInstance());
        t.set("GetServer",new GetServer());
        t.set("GetWorlds",new GetWorlds());
        t.set("GetWorldKeys",new GetWorldKeys());
        t.set("GetWorld",new GetWorld());
        t.set("GetModTime",new GetModTime());
        t.set("GetServerTime",new GetServerTime());
        t.set("GetCurrentWorld",new GetCurrentWorld());

        env.set("MAPI", t);

        if (!env.get("package").isnil()) env.get("package").get("loaded").set("MAPI", t);

        return NIL;
    }

    static class Msg extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            if (lualoader.errorinchat) {
                Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(arg.tojstring()));
            }
            else {
                lualoader.msglist.add(arg.tojstring());
            }

            return NONE;
        }
    }

    static class IsDedicatedServer extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            return LuaValue.valueOf(FMLEnvironment.dist == Dist.DEDICATED_SERVER);
        }
    }

    static class IsClient extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            return LuaValue.valueOf(FMLEnvironment.dist == Dist.CLIENT);
        }
    }

    static class GetLuaItem extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            return CoerceJavaToLua.coerce(lualoader.registerslist.get("ITEM").get(arg.arg1().tojstring()));
        }
    }

    static class GetLuaItemGroup extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            String errname = "Init";

            try {
                lualoader.lua.get("Register").get("OnRegister").call();
                lualoader.lua.get("Register").get("GetList").call().get("ITEMGROUP").get(arg.tojstring()).checktable();

                errname = "ITEMGROUP."+arg.tojstring();

                if (!lualoader.luaitemgrouplist.containsKey(arg.tojstring())){
                    lualoader.luaitemgrouplist.put(arg.tojstring(),new luaitemgroup(arg.tojstring()));
                }

                return CoerceJavaToLua.coerce(lualoader.luaitemgrouplist.get(arg.tojstring()));
            }
            catch (Exception e){
                lualoader.LuaError(e,"MAPI:GetLuaItemGroup:"+errname);

                return NIL;
            }
        }
    }

    static class GetMinecraftInstance extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            return CoerceJavaToLua.coerce(Minecraft.getInstance());
        }
    }

    static class GetServer extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            if (lualoader.server == null) return NIL;

            return CoerceJavaToLua.coerce(lualoader.server);
        }
    }

    static class GetWorlds extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            if (lualoader.server == null) return NIL;

            LuaValue t = LuaValue.tableOf();
            int k = 1;

            for (ServerWorld w:lualoader.server.getWorlds()){
                t.set(k,CoerceJavaToLua.coerce(w));
                k++;
            }

            return t;
        }
    }

    static class GetWorldKeys extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            if (lualoader.server == null) return NIL;

            LuaValue t = LuaValue.tableOf();

            for (Map.Entry<RegistryKey<World>,ServerWorld> entry:lualoader.server.worlds.entrySet()){
                t.set(CoerceJavaToLua.coerce(entry.getKey()),CoerceJavaToLua.coerce(entry.getValue()));
            }

            return t;
        }
    }

    static class GetWorld extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            return CoerceJavaToLua.coerce(lualoader.server.getWorld((RegistryKey<World>)arg.touserdata(RegistryKey.class)));
        }
    }

    static class GetModTime extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            return LuaValue.valueOf(lualoader.modtime);
        }
    }

    static class GetServerTime extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            if (lualoader.server == null) return NIL;

            return LuaValue.valueOf(lualoader.server.getTickCounter());
        }
    }

    static class GetCurrentWorld extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            if (lualoader.world == null) return NIL;

            return CoerceJavaToLua.coerce(lualoader.world);
        }
    }
}
