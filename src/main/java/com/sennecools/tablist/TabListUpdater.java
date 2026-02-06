package com.sennecools.tablist;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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

    // Animation frame indices and tick counter
    private int headerFrameIndex = 0;
    private int footerFrameIndex = 0;
    private int animationTickCounter = 0;

    // AFK tracking: stores last known X, Z, yRot (skip Y to avoid gravity false positives)
    private final Map<UUID, double[]> lastPlayerPositions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActivityTime = new ConcurrentHashMap<>();

    public TabListUpdater() {
        INSTANCE = this;
    }

    /**
     * Called after each server tick to determine if it's time to refresh the tab list.
     *
     * @param event the server post-tick event
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }

        // Increment tick counter and check if update interval reached
        ticksSinceLastUpdate++;
        int updateIntervalTicks = Config.updateInterval / 50; // convert ms to ticks
        if (ticksSinceLastUpdate < updateIntervalTicks) {
            return;
        }
        // Reset counter for next cycle
        ticksSinceLastUpdate = 0;

        // Advance animation frames based on animation_interval
        animationTickCounter++;
        if (animationTickCounter >= Config.animationInterval) {
            animationTickCounter = 0;
            List<String> headerFrames = Config.headerFrames;
            List<String> footerFrames = Config.footerFrames;
            if (headerFrames != null && headerFrames.size() > 1) {
                headerFrameIndex = (headerFrameIndex + 1) % headerFrames.size();
            }
            if (footerFrames != null && footerFrames.size() > 1) {
                footerFrameIndex = (footerFrameIndex + 1) % footerFrames.size();
            }
        }

        // Update AFK tracking for all players
        if (Config.afkEnabled) {
            updateAFKTracking(server);
        }

        updateSorting(server);

        // Broadcast refresh to all connected players
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

    /**
     * Returns whether the given player is considered AFK.
     */
    public boolean isPlayerAFK(ServerPlayer player) {
        if (!Config.afkEnabled) return false;
        Long lastActive = lastActivityTime.get(player.getUUID());
        if (lastActive == null) return false;
        return (System.currentTimeMillis() - lastActive) >= Config.afkTimeout * 1000L;
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            lastActivityTime.put(player.getUUID(), System.currentTimeMillis());
        }
    }

    private void refreshPlayerTab(ServerPlayer player) {
        List<String> headerFrames = Config.headerFrames;
        List<String> footerFrames = Config.footerFrames;

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

        // Triggers the TabListNameFormat event and broadcasts UPDATE_DISPLAY_NAME
        // packet if the name changed (NeoForge handles the diffing internally)
        player.refreshTabListName();
    }

    private void updateSorting(MinecraftServer server) {
        String mode = Config.sortMode;
        if (mode == null) {
            mode = "NONE";
        }
        mode = mode.toUpperCase();

        Scoreboard scoreboard = server.getScoreboard();

        // Transition from active sorting to NONE: clean up all tl_ teams
        if ("NONE".equals(mode)) {
            if (lastSortMode != null && !"NONE".equals(lastSortMode)) {
                removeAllTlTeams(scoreboard);
            }
            // First tick: clean up any leftover tl_ teams from previous sessions
            if (lastSortMode == null) {
                removeAllTlTeams(scoreboard);
            }
            lastSortMode = mode;
            return;
        }

        // First tick cleanup for non-NONE modes too
        if (lastSortMode == null) {
            removeAllTlTeams(scoreboard);
        }

        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());

        // Build the comparator based on mode
        Comparator<ServerPlayer> comparator;
        if ("RANK".equals(mode)) {
            comparator = Comparator
                    .comparingInt((ServerPlayer p) -> TabListVariables.getPlayerRankPower(p))
                    .reversed()
                    .thenComparing(p -> p.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER);
        } else {
            // ALPHABETICAL (or any unrecognized mode)
            comparator = Comparator.comparing(
                    p -> p.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER);
        }

        players.sort(comparator);

        // Track which tl_ teams are needed this cycle
        int index = 0;
        List<String> neededTeams = new ArrayList<>();

        for (ServerPlayer player : players) {
            String playerName = player.getGameProfile().getName();

            // Skip players on non-tl_ teams (don't break other mods' teams)
            PlayerTeam currentTeam = scoreboard.getPlayersTeam(playerName);
            if (currentTeam != null && !currentTeam.getName().startsWith(TEAM_PREFIX)) {
                continue;
            }

            index++;
            String teamName = String.format("%s%05d", TEAM_PREFIX, index);
            neededTeams.add(teamName);

            // Skip if player is already on the correct tl_ team
            if (currentTeam != null && currentTeam.getName().equals(teamName)) {
                continue;
            }

            // Remove from old tl_ team if on one
            if (currentTeam != null) {
                scoreboard.removePlayerFromTeam(playerName, currentTeam);
            }

            // Create team if it doesn't exist
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team == null) {
                team = scoreboard.addPlayerTeam(teamName);
                team.setNameTagVisibility(PlayerTeam.Visibility.ALWAYS);
                team.setPlayerPrefix(Component.empty());
                team.setPlayerSuffix(Component.empty());
            }

            scoreboard.addPlayerToTeam(playerName, team);
        }

        // Remove stale tl_ teams that are no longer needed
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

    /**
     * Fired by NeoForge whenever a player's tab list display name is resolved.
     * Sets the display name based on FTB Ranks formatting or the config template.
     */
    @SubscribeEvent
    public void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String displayName = TabListVariables.resolveDisplayName(player);
            event.setDisplayName(Component.literal(displayName));
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        lastSentContent.remove(uuid);
        lastPlayerPositions.remove(uuid);
        lastActivityTime.remove(uuid);

        // Clean up the player's tl_ team on logout
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                Scoreboard scoreboard = server.getScoreboard();
                String playerName = player.getGameProfile().getName();
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
}
