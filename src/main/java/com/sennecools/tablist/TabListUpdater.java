package com.sennecools.tablist;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sennecools.tablist.config.TabListConfig;
import com.sennecools.tablist.platform.Services;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TabListUpdater {

    public static TabListUpdater INSTANCE;

    private static final String TEAM_PREFIX = "tl_";

    private int ticksSinceLastUpdate = 0;
    private final Map<UUID, String> lastSentContent = new ConcurrentHashMap<>();
    private String lastSortMode = null;

    private int headerFrameIndex = 0;
    private int footerFrameIndex = 0;
    private int animationTickCounter = 0;

    private final Map<UUID, double[]> lastPlayerPositions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActivityTime = new ConcurrentHashMap<>();

    public TabListUpdater() {
        INSTANCE = this;
    }

    public void resetState() {
        lastSentContent.clear();
        headerFrameIndex = 0;
        footerFrameIndex = 0;
        animationTickCounter = 0;
        ticksSinceLastUpdate = 0;
        lastSortMode = null;
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildReloadCommand() {
        return Commands.literal("tablist")
                //? if >=1.21.11 {
                /*.requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))*/
                //?} else {
                .requires(source -> source.hasPermission(2))
                //?}
                .then(Commands.literal("reload")
                        .executes(context -> {
                            TabListConfig.load();
                            resetState();
                            context.getSource().sendSuccess(() -> Component.literal("\u00A7aTabList config reloaded."), true);
                            return 1;
                        })
                );
    }

    public void onServerTick(MinecraftServer server) {
        if (server == null) {
            return;
        }

        ticksSinceLastUpdate++;
        int updateIntervalTicks = TabListConfig.updateInterval / 50;
        if (ticksSinceLastUpdate < updateIntervalTicks) {
            return;
        }
        ticksSinceLastUpdate = 0;

        animationTickCounter++;
        if (animationTickCounter >= TabListConfig.animationInterval) {
            animationTickCounter = 0;
            List<String> headerFrames = TabListConfig.headerFrames;
            List<String> footerFrames = TabListConfig.footerFrames;
            if (headerFrames != null && headerFrames.size() > 1) {
                headerFrameIndex = (headerFrameIndex + 1) % headerFrames.size();
            }
            if (footerFrames != null && footerFrames.size() > 1) {
                footerFrameIndex = (footerFrameIndex + 1) % footerFrames.size();
            }
        }

        if (TabListConfig.afkEnabled) {
            updateAFKTracking(server);
        }

        updateSorting(server);

        server.getPlayerList().getPlayers().forEach(this::refreshPlayerTab);
    }

    private void updateAFKTracking(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            double x = player.getX();
            double z = player.getZ();
            double yRot = player.getYRot();

            double[] lastPos = lastPlayerPositions.get(uuid);
            if (lastPos == null || lastPos[0] != x || lastPos[1] != z || lastPos[2] != yRot) {
                lastPlayerPositions.put(uuid, new double[]{x, z, yRot});
                lastActivityTime.put(uuid, now);
            } else if (!lastActivityTime.containsKey(uuid)) {
                lastActivityTime.put(uuid, now);
            }
        }
    }

    public boolean isPlayerAFK(ServerPlayer player) {
        if (!TabListConfig.afkEnabled) return false;
        Long lastActive = lastActivityTime.get(player.getUUID());
        if (lastActive == null) return false;
        return (System.currentTimeMillis() - lastActive) >= TabListConfig.afkTimeout * 1000L;
    }

    public void onPlayerChat(ServerPlayer player) {
        if (player != null) {
            lastActivityTime.put(player.getUUID(), System.currentTimeMillis());
        }
    }

    private void refreshPlayerTab(ServerPlayer player) {
        List<String> headerFrames = TabListConfig.headerFrames;
        List<String> footerFrames = TabListConfig.footerFrames;

        String headerTemplate = (headerFrames != null && !headerFrames.isEmpty())
                ? headerFrames.get(headerFrameIndex % headerFrames.size())
                : "";
        String footerTemplate = (footerFrames != null && !footerFrames.isEmpty())
                ? footerFrames.get(footerFrameIndex % footerFrames.size())
                : "";

        String header = TabListVariables.tablistChars(headerTemplate, player);
        String footer = TabListVariables.tablistChars(footerTemplate, player);

        String combined = header + "\0" + footer;
        String previous = lastSentContent.put(player.getUUID(), combined);
        if (!combined.equals(previous)) {
            ClientboundTabListPacket packet =
                new ClientboundTabListPacket(
                    Component.literal(header),
                    Component.literal(footer)
                );
            player.connection.send(packet);
        }

        Services.PLATFORM.refreshDisplayName(player);
    }

    private void updateSorting(MinecraftServer server) {
        String mode = TabListConfig.sortMode;
        if (mode == null) {
            mode = "NONE";
        }
        mode = mode.toUpperCase();

        Scoreboard scoreboard = server.getScoreboard();

        if ("NONE".equals(mode)) {
            if (lastSortMode != null && !"NONE".equals(lastSortMode)) {
                removeAllTlTeams(scoreboard);
            }
            if (lastSortMode == null) {
                removeAllTlTeams(scoreboard);
            }
            lastSortMode = mode;
            return;
        }

        if (lastSortMode == null) {
            removeAllTlTeams(scoreboard);
        }

        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());

        Comparator<ServerPlayer> comparator;
        //? if >=1.21.9 {
        /*if ("RANK".equals(mode)) {
            comparator = Comparator
                    .comparingInt((ServerPlayer p) -> TabListVariables.getPlayerRankPower(p))
                    .reversed()
                    .thenComparing(p -> p.getGameProfile().name(), String.CASE_INSENSITIVE_ORDER);
        } else {
            comparator = Comparator.comparing(
                    p -> p.getGameProfile().name(), String.CASE_INSENSITIVE_ORDER);
        }*/
        //?} else {
        if ("RANK".equals(mode)) {
            comparator = Comparator
                    .comparingInt((ServerPlayer p) -> TabListVariables.getPlayerRankPower(p))
                    .reversed()
                    .thenComparing(p -> p.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER);
        } else {
            comparator = Comparator.comparing(
                    p -> p.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER);
        }
        //?}

        players.sort(comparator);

        int index = 0;
        List<String> neededTeams = new ArrayList<>();

        for (ServerPlayer player : players) {
            //? if >=1.21.9 {
            /*String playerName = player.getGameProfile().name();*/
            //?} else {
            String playerName = player.getGameProfile().getName();
            //?}

            PlayerTeam currentTeam = scoreboard.getPlayersTeam(playerName);
            if (currentTeam != null && !currentTeam.getName().startsWith(TEAM_PREFIX)) {
                continue;
            }

            index++;
            String teamName = String.format("%s%05d", TEAM_PREFIX, index);
            neededTeams.add(teamName);

            if (currentTeam != null && currentTeam.getName().equals(teamName)) {
                continue;
            }

            if (currentTeam != null) {
                scoreboard.removePlayerFromTeam(playerName, currentTeam);
            }

            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team == null) {
                team = scoreboard.addPlayerTeam(teamName);
                team.setNameTagVisibility(PlayerTeam.Visibility.ALWAYS);
                team.setPlayerPrefix(Component.empty());
                team.setPlayerSuffix(Component.empty());
            }

            scoreboard.addPlayerToTeam(playerName, team);
        }

        for (PlayerTeam team : List.copyOf(scoreboard.getPlayerTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX) && !neededTeams.contains(team.getName())) {
                scoreboard.removePlayerTeam(team);
            }
        }

        lastSortMode = mode;
    }

    private void removeAllTlTeams(Scoreboard scoreboard) {
        for (PlayerTeam team : List.copyOf(scoreboard.getPlayerTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                scoreboard.removePlayerTeam(team);
            }
        }
    }

    public void onPlayerLogout(ServerPlayer player) {
        UUID uuid = player.getUUID();
        lastSentContent.remove(uuid);
        lastPlayerPositions.remove(uuid);
        lastActivityTime.remove(uuid);

        //? if >=1.21.9 {
        /*MinecraftServer server = player.level().getServer();*/
        //?} else {
        MinecraftServer server = player.getServer();
        //?}
        if (server != null) {
            Scoreboard scoreboard = server.getScoreboard();
            //? if >=1.21.9 {
            /*String playerName = player.getGameProfile().name();*/
            //?} else {
            String playerName = player.getGameProfile().getName();
            //?}
            PlayerTeam team = scoreboard.getPlayersTeam(playerName);
            if (team != null && team.getName().startsWith(TEAM_PREFIX)) {
                scoreboard.removePlayerFromTeam(playerName, team);
                if (team.getPlayers().isEmpty()) {
                    scoreboard.removePlayerTeam(team);
                }
            }
        }
    }
}
