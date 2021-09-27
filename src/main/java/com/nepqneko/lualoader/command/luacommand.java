package com.nepqneko.lualoader.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

public class luacommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher){
        dispatcher.register(Commands.literal("lua_run").requires(source -> source.hasPermissionLevel(2))
                .then(Commands.argument("luacode", StringArgumentType.word()))
                .executes(source -> runcode(source.getSource(),StringArgumentType.getString(source,"luacode"))));
        dispatcher.register(Commands.literal("lua_reload").requires(source -> source.hasPermissionLevel(2)).executes(source -> reloadlua(source.getSource())));
    }

    public static int runcode(CommandSource source, String code){
        com.nepqneko.lualoader.lualoader.lua.load(code).call();

//        CommandContext<CommandSource> context
//        context.getSource().sendFeedback(new StringTextComponent(StringArgumentType.getString(context,"luacode")), false);
//        context.getSource().sendFeedback(new StringTextComponent(code), false);

        return 2;
    }

    public static int reloadlua(CommandSource source){
        source.sendFeedback(new StringTextComponent("Lua Reloading..."), false);
        com.nepqneko.lualoader.lualoader.ReloadLua();
        source.sendFeedback(new StringTextComponent("Lua Reloading Complete"), false);

        return 1;
    }
}
