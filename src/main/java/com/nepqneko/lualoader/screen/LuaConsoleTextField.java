package com.nepqneko.lualoader.screen;

import com.nepqneko.lualoader.lualoader;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;

public class LuaConsoleTextField extends TextFieldWidget {
    private int oci = lualoader.oldcommands.size()-1;
    private int oci2 = -1;

    public LuaConsoleTextField(FontRenderer font, int x, int y, int w, int h, ITextComponent title){
        super(font,x,y,w,h,title);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        if (!this.canWrite()) {
            return false;
        }
        else {
            switch (keyCode){
                case 335:
                case 257:{
                    lualoader.LuaLogsAdd(getText());

                    String[] com = getText().split(" ");

                    if (com.length > 0){
                        String name = com[0];
                        String[] args = getText().substring(name.length()).split(" ");

                        if (com.length > 1){
                            lualoader.RunCommand(name,args);
                        }
                        else {
                            lualoader.RunCommand(name);
                        }

                        if (!getText().equals("")){
                            lualoader.oldcommands.add(getText());
                            oci = lualoader.oldcommands.size()-1;
                            oci2 = -1;
                        }
                    }

                    setText("");

                    return true;
                }
                case 265:{
                    if (oci < 0) return true;

                    setText(lualoader.oldcommands.get(oci));

                    if (oci > 0){
                        oci2 = oci;
                        oci--;
                    }

                    return true;
                }
                case 264:{
                    if (oci2 < 0){
                        setText("");
                        oci = lualoader.oldcommands.size()-1;

                        return true;
                    }

                    setText(lualoader.oldcommands.get(oci2));

                    if (oci2 < lualoader.oldcommands.size()-1){
                        oci2++;
                        oci = oci2;
                    }
                    else {
                        oci2 = -1;
                    }

                    return true;
                }
            }
        }

        return super.keyPressed(keyCode,scanCode,modifiers);
    }
}
