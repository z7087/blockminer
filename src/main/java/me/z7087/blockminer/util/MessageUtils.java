package me.z7087.blockminer.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class MessageUtils {
    private MessageUtils() {}

    public static void printMessage(Text message) {
        //#if MC >= 260200
        //$$ MinecraftClient.getInstance().guiManager.inGameHud.getChatHud().addClientSystemMessage(message);
        //#else
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message);
        //#endif
    }
}
