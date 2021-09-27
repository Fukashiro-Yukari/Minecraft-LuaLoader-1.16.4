package com.nepqneko.lualoader.lualib;

import com.nepqneko.lualoader.lualoader;
import org.luaj.lualoader.vm2.LuaValue;
import org.luaj.lualoader.vm2.Varargs;

public class LuaJavaThread extends Thread {
    private Thread t;
    private final String threadName;
    private final LuaValue func;
    private Varargs ret;
    private boolean isover;

    public LuaJavaThread(String name,LuaValue func){
        this.threadName = name;
        this.func = func;
    }

    @Override
    public void run(){
        try {
            ret = func.call();
        }
        catch (Exception e){
            lualoader.LuaError(e);
        }
        finally {
            isover = true;
        }
    }

    public Varargs getresult(){
        return ret;
    }

    public LuaValue isdone(){
        return LuaValue.valueOf(isover);
    }

    @Override
    public void start(){
        if (t == null){
            t = new Thread(this,threadName);
            t.start();
        }
        else {
            isover = false;

            t.start();
        }
    }
}
