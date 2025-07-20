package com.zerog.neoessentials.commands;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.MailManager;
import com.zerog.neoessentials.utils.TextUtil;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
        player.sendSystemMessage(Component.literal(TextUtil.formatText(
                "&eWelcome, &6" + playerName + "&e!"
        )));
        player.sendSystemMessage(Component.literal(TextUtil.formatText(
                "&cYou have &4" + unreadMessages + " &cnew messages! Type &d/inbox &cto view your mail."
        )));
        player.sendSystemMessage(Component.literal(TextUtil.formatText(
                "&6Players online: &c" + onlinePlayers
        )));
        player.sendSystemMessage(Component.literal(TextUtil.formatText(
                "&6Server time: &c" + serverTime
        )));
        player.sendSystemMessage(Component.literal(TextUtil.formatText(
                "&eType &d/help &efor a list of commands."
        )));
    }
}