package com.drypted.lifesteal.gui;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;

public class ReviveGUI {
    public static final Component TITLE = Component.literal("Revive a Player");

    public static class ReviveContainer extends SimpleContainer {
        public ReviveContainer(int size) {
            super(size);
        }
    }

    public static void open(ServerPlayer player) {
        ReviveContainer container = new ReviveContainer(54);
        MinecraftServer server = player.level().getServer();

        int slot = 0;

        for (UserBanListEntry entry : server.getPlayerList().getBans().getEntries()) {
            if (slot >= 54) break;
            GameProfile profile = new GameProfile(entry.getUser().id(), entry.getUser().name());
            container.setItem(slot++, createHead(profile));
        }

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (slot >= 54) break;
            if (p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR && !server.getPlayerList().getBans().isBanned(p.nameAndId())) {
                container.setItem(slot++, createHead(p.getGameProfile()));
            }
        }

        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, playerEntity) -> 
                new ChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6), 
            TITLE
        ));
    }

    private static ItemStack createHead(GameProfile profile) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(
            DataComponents.PROFILE,
            ResolvableProfile.createResolved(profile)
        );
        
        head.set(DataComponents.CUSTOM_NAME, Component.literal("§eRevive " + profile.name()).withStyle(style -> style.withItalic(false)));
        return head;
    }
}