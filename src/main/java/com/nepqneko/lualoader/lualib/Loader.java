package com.nepqneko.lualoader.lualib;

import org.apache.commons.io.FilenameUtils;
import org.luaj.lualoader.vm2.LuaTable;
import org.luaj.lualoader.vm2.LuaValue;
import org.luaj.lualoader.vm2.Varargs;
import org.luaj.lualoader.vm2.lib.OneArgFunction;
import org.luaj.lualoader.vm2.lib.TwoArgFunction;
import org.luaj.lualoader.vm2.lib.VarArgFunction;
import org.luaj.lualoader.vm2.lib.jse.CoerceJavaToLua;
import com.nepqneko.lualoader.lualoader;

import java.io.File;

public class Loader extends TwoArgFunction {
    public LuaValue call(LuaValue modname, LuaValue env){
        LuaTable t = new LuaTable();

        t.set("Include", new Include());
        t.set("GetLoaderClass",new GetLoaderClass());
        t.set("CreateUserdata",new CreateUserdata());

        env.set("Loader",t);
        env.set("include",new Include());

        if (!env.get("package").isnil()) env.get("package").get("loaded").set("Loader", t);

        return NIL;
    }

    static class Include extends VarArgFunction {
        public Varargs invoke(Varargs args){
            String path = args.arg1().tojstring();

            if (!FilenameUtils.getExtension(path).equals("lua")) path = path+".lua";

            File f = new File("lua/"+path);

            if (f.exists() && f.isFile()){
                String ext = FilenameUtils.getExtension(f.getName());

                if (ext.equals("lua")){
                    try {
                        lualoader.reloadfiles.put(f,f.lastModified());

                        return lualoader.lua.loadfile(f.getPath()).invoke(args.subargs(2));
                    }
                    catch (Exception e){
                        lualoader.LuaError(e,"Include:"+f.getPath());
                    }
                }
            }

            return NIL;
        }
    }

    static class GetLoaderClass extends OneArgFunction {
        public LuaValue call(LuaValue arg){
            return CoerceJavaToLua.coerce(lualoader.class);
        }
    }

    static class CreateUserdata extends VarArgFunction {
        public LuaValue invoke(Varargs args){
            return new LuaCustomUserdata(args.checktable(1));
        }
    }
}
