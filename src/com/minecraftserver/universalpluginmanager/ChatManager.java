package com.minecraftserver.universalpluginmanager;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatManager implements Listener {
    private final UniversalPluginManager plugin;
    private boolean                      chatON;
    private boolean                      request;
    private int                          type;
    private Player                       player;

    public ChatManager(UniversalPluginManager parent) {
        plugin = parent;
        chatON = true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onplayerChat(AsyncPlayerChatEvent e) {
        if (!chatON && !e.isCancelled()) {
            if (e.getPlayer().getUniqueId() == player.getUniqueId()) {
                if (request) if (e.getMessage().equalsIgnoreCase("yes")) {
                    sendMessage(ChatColor.GREEN + "Action confirmed!");
                    executeAction();
                } else {
                    sendMessage(ChatColor.RED + "Action aborted!");
                    request = false;
                    type = -1;
                }
                e.setCancelled(true);
            } else if (player != null) e.getRecipients().remove(player);
        }

    }

    private void executeAction() {
        switch (type) {
        case 1:
            plugin.loadCFG();
            break;
        case 2:
            plugin.changeValue();
            break;
        default:
            sendMessage(ChatColor.RED + "Error while reading chat");
        }
        type = -1;
        request = false;

    }

    void disableChat(Player player) {
        chatON = false;
        this.player = player;
    }

    void enableChat() {
        chatON = true;
        request = false;
    }

    /*
     * Type:
     * 1: load config
     * 2: change config value
     */
    void requestConfirmation(int type) {
        this.type = type;
        request = true;

    }

    void sendMessage(String message) {
        if (!chatON && player != null && message != null)
            player.sendMessage(message);
    }
}
