package com.nepqneko.lualoader.lualib;

import org.luaj.lualoader.vm2.LuaUserdata;
import org.luaj.lualoader.vm2.LuaValue;

public class LuaCustomUserdata extends LuaUserdata {
    private LuaValue tbl;
    private int typeid = -1;

    public LuaCustomUserdata(LuaValue tbl){
        super("Custom User data","");

        this.tbl = tbl;
    }

    public String tojstring() {
        return String.valueOf(tbl.get("tostring").isnil() ? "Custom User data" : tbl.get("tostring").tojstring());
    }

    public int type() {
        if (!tbl.get("type").isnil()){
            if (typeid == -1){
                TYPE_NAMES[TYPE_NAMES.length+1] = tbl.get("type").tojstring();
                typeid = TYPE_NAMES.length+1;
            }

            return typeid;
        }

        return LuaValue.TUSERDATA;
    }

    public String typename() {
        return tbl.get("type").isnil() ? "userdata" : tbl.get("type").tojstring();
    }

    public LuaValue get(LuaValue key){
        return tbl.get("get").checkfunction().call(tbl,key);
    }

    public void set(LuaValue key,LuaValue value){
        tbl.get("set").checkfunction().call(tbl,key,value);
    }
}
