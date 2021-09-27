package com.nepqneko.lualoader.networking;

import net.minecraft.network.PacketBuffer;
import org.luaj.lualoader.vm2.LuaValue;
import org.luaj.lualoader.vm2.Varargs;

public class LuaSendPack {
    private Varargs args;

    public LuaSendPack(PacketBuffer buffer){
        int argn = buffer.readInt();

        for (int i = 1;i < argn;i++){

        }
    }

    public LuaSendPack(Varargs args){
        this.args = args;
    }

    public void toBytes(PacketBuffer buffer){
        buffer.writeInt(args.narg());

        for (int i = 1;i < args.narg();i++){
            LuaValue arg = args.arg(i);

            if (arg.isboolean()){
                buffer.writeBoolean(arg.toboolean());
            }
        }
    }
}
