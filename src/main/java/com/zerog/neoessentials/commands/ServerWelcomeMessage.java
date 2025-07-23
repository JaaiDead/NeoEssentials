package com.zerog.neoessentials.commands;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.MailManager;
import com.zerog.neoessentials.utils.TextUtil;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@EventBusSubscriber
public class ServerWelcomeMessage {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerWelcomeMessage.sendWelcome(player);

    }

    public static void sendWelcome(ServerPlayer player) {
        String playerName = player.getScoreboardName();

        // Get mail info
        MailManager mailManager = NeoEssentials.getInstance().getDataManager().getMailManager();
        int unreadMessages = mailManager.getUnreadMailCount(player);

        // Get online players
        int onlinePlayers = player.server.getPlayerCount();

        // Get server/system time
        LocalTime now = LocalTime.now();
        String serverTime = now.format(DateTimeFormatter.ofPattern("h:mm a"));

        // Send messages (with color formatting)
        String welcomeMessage = "&6Welcome, &e" + playerName + "&6!\n"
                + "&6You have &c" + unreadMessages + "&6 new messages! Type &c/mail&6 to view your mail.\n"
                + "&6Players online: &c" + onlinePlayers + "\n"
                + "&6World time: &c" + serverTime + "&6.\n\n"
                + "&6Type &c/help&6 for a list of available commands.";

        player.sendSystemMessage(Component.literal(TextUtil.formatText(welcomeMessage)));
    }
}