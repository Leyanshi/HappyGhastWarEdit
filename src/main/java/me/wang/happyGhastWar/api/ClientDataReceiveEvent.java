package me.wang.happyGhastWar.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ClientDataReceiveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final String clientName;
    private final String serverAddress;
    private final String data;
    private boolean cancelled = false;

    public ClientDataReceiveEvent(String clientName, String serverAddress, String data) {
        this.clientName = clientName;
        this.serverAddress = serverAddress;
        this.data = data;
    }

    public String getClientName() {
        return clientName;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getData() {
        return data;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}