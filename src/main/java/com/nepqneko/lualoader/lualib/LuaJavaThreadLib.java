package com.nepqneko.lualoader.lualib;

import com.nepqneko.lualoader.lualoader;
import org.luaj.lualoader.vm2.LuaTable;
import org.luaj.lualoader.vm2.LuaValue;
import org.luaj.lualoader.vm2.Varargs;
import org.luaj.lualoader.vm2.lib.TwoArgFunction;
import org.luaj.lualoader.vm2.lib.VarArgFunction;
import org.luaj.lualoader.vm2.lib.jse.CoerceJavaToLua;

import java.util.HashMap;

public class LuaJavaThreadLib extends TwoArgFunction {
    private static HashMap<String,LuaJavaThread> LuaJavaThreadList = new HashMap<String,LuaJavaThread>();

    public LuaValue call(LuaValue modname, LuaValue env){
        LuaTable t = new LuaTable();

        t.set("create", new create());
        t.set("currentThread",new currentThread());
        t.set("yield",new yield());
        t.set("sleep",new sleep());
        t.set("interrupted",new interrupted());
        t.set("activeCount",new activeCount());
        t.set("enumerate",new enumerate());
        t.set("dumpStack",new dumpStack());
        t.set("holdsLock",new holdsLock());
        t.set("getAllStackTraces",new getAllStackTraces());
        t.set("setDefaultUncaughtExceptionHandler",new setDefaultUncaughtExceptionHandler());
        t.set("getDefaultUncaughtExceptionHandler",new getDefaultUncaughtExceptionHandler());
        t.set("MIN_PRIORITY",Thread.MIN_PRIORITY);
        t.set("NORM_PRIORITY",Thread.NORM_PRIORITY);
        t.set("MAX_PRIORITY",Thread.MAX_PRIORITY);
        t.set("State",CoerceJavaToLua.coerce(Thread.State.class));

        env.set("thread",t);

        if (!env.get("package").isnil()) env.get("package").get("loaded").set("MAPI", t);

        return NIL;
    }

    public class create extends VarArgFunction {
        public Varargs invoke(Varargs args){
            LuaTable t = new LuaTable();
            LuaJavaThread thread = new LuaJavaThread(args.checkjstring(1),args.checkfunction(2));

            LuaJavaThreadList.put(args.arg1().checkjstring(),thread);

            LuaTable meta = new LuaTable();

            meta.set("__index",t);
            meta.set("__newindex",t);

            return CoerceJavaToLua.coerce(thread).setmetatable(meta);
        }
    }

    public class get extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return CoerceJavaToLua.coerce(LuaJavaThreadList.get(args.checkjstring(1)));
        }
    }

    public class start extends VarArgFunction {
        LuaJavaThread thread;

        start(LuaJavaThread thread){
            this.thread = thread;
        }

        public Varargs invoke(Varargs args){
            thread.start();

            return NIL;
        }
    }

    public class currentThread extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return CoerceJavaToLua.coerce(Thread.currentThread());
        }
    }

    public class yield extends VarArgFunction {
        public Varargs invoke(Varargs args){
            Thread.yield();

            return NIL;
        }
    }

    public class sleep extends VarArgFunction {
        public Varargs invoke(Varargs args){
            switch (args.narg()){
                case 1:
                    try {
                        Thread.sleep(args.checklong(1));
                    } catch (InterruptedException e) {
                        lualoader.LuaError(e);
                    }

                    break;
                case 2:
                    try {
                        Thread.sleep(args.checklong(1),args.checkint(2));
                    } catch (InterruptedException e){
                        lualoader.LuaError(e);
                    }

                    break;
            }

            return NIL;
        }
    }

    public class interrupted extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return LuaValue.valueOf(Thread.interrupted());
        }
    }

    public class activeCount extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return LuaValue.valueOf(Thread.activeCount());
        }
    }

    public class enumerate extends VarArgFunction {
        public Varargs invoke(Varargs args){
            LuaTable tbl = args.checktable(1);
            LuaValue k = LuaValue.NIL;
            Thread[] th = new Thread[tbl.length()];
            int i = 0;

            while (true) {
                if (tbl == null) break;

                Varargs n = tbl.next(k);

                if ((k = n.arg1()).isnil()) break;

                LuaValue v = n.arg(2);
                i++;

                th[i] = (Thread)v.touserdata(Thread.class);
            }

            return LuaValue.valueOf(Thread.enumerate(th));
        }
    }

    public class dumpStack extends VarArgFunction {
        public Varargs invoke(Varargs args){
            lualoader.printStackTrace(new Exception("Stack trace"));

            return NIL;
        }
    }

    public class holdsLock extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return LuaValue.valueOf(Thread.holdsLock(args.arg1()));
        }
    }

    public class getAllStackTraces extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return CoerceJavaToLua.coerce(Thread.getAllStackTraces());
        }
    }

    public class setDefaultUncaughtExceptionHandler extends VarArgFunction {
        public Varargs invoke(Varargs args){
            Thread.setDefaultUncaughtExceptionHandler((Thread.UncaughtExceptionHandler)args.arg1().checkuserdata(Thread.UncaughtExceptionHandler.class));

            return NIL;
        }
    }

    public class getDefaultUncaughtExceptionHandler extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return CoerceJavaToLua.coerce(Thread.getDefaultUncaughtExceptionHandler());
        }
    }
}
