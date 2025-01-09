package me.z7087.blockminer;

import net.minecraft.text.Text;

public final class I18n {
    private I18n() {}

    private static Text ofTranslatable(String key) {
        //#if MC >= 11900
        return Text.translatable(key);
        //#else
        //$$ return new net.minecraft.text.TranslatableText(key);
        //#endif
    }

    public static final Text TOGGLE_ON = ofTranslatable("blockminer.toggle.on");
    public static final Text TOGGLE_OFF = ofTranslatable("blockminer.toggle.off");

    public static final Text WARN_MULTIPLAYER = ofTranslatable("blockminer.warn.multiplayer");
}
