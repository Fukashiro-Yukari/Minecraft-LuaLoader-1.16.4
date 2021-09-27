package com.nepqneko.lualoader.lualib;

import org.luaj.lualoader.vm2.LuaTable;
import org.luaj.lualoader.vm2.LuaValue;
import org.luaj.lualoader.vm2.Varargs;
import org.luaj.lualoader.vm2.lib.TwoArgFunction;
import org.luaj.lualoader.vm2.lib.VarArgFunction;

public class net extends TwoArgFunction{
    public LuaValue call(LuaValue modname, LuaValue env){
        LuaTable t = new LuaTable();

        t.set("Send", new Send());
        t.set("SendToServer",new SendToServer());

        env.set("netj",t);

        if (!env.get("package").isnil()) env.get("package").get("loaded").set("netj", t);

        return NIL;
    }

    class Send extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return NIL;
        }
    }

    class SendToServer extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return NIL;
        }
    }
}
