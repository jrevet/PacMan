package fr.azuxul.pacman;

import fr.azuxul.pacman.player.PlayerPacMan;
import fr.azuxul.pacman.scoreboard.ScoreboardPacMan;
import fr.azuxul.pacman.timer.TimerPacMan;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * GameManager for PacMan plugin
 *
 * @author Azuxul
 * @version 1.0
 */
public class GameManager {

    private Server server;
    private Logger logger;
    private Plugin plugin;
    private TimerPacMan timer;
    private ScoreboardPacMan scoreboard;
    private List<PlayerPacMan> playerPacManList;
    private boolean start, end, maxPlayer, minPlayer;
    private int globalCoins;

    public GameManager(Logger logger, Plugin plugin, Server server) {

        this.server = server;
        this.logger = logger;
        this.plugin = plugin;
        this.scoreboard = new ScoreboardPacMan(ChatColor.YELLOW + "Pac-Man", this);
        this.timer = new TimerPacMan(this);
        this.playerPacManList = new ArrayList<>();
    }

    public void updatePlayerNb() {

        int players = server.getOnlinePlayers().size();

        // TODO: Set to 6
        if (players >= 3) { // If player nb is >= 6
            minPlayer = true; // Stet min player to true
            maxPlayer = players >= 10; // Set max player to player nb >= 10
        } else
            minPlayer = false; // Else set to false
    }

    public List<PlayerPacMan> getPlayerPacManList() {
        return playerPacManList;
    }

    public TimerPacMan getTimer() {
        return timer;
    }

    public ScoreboardPacMan getScoreboard() {
        return scoreboard;
    }

    public Server getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean isStart() {
        return start;
    }

    public boolean isEnd() {
        return end;
    }

    public boolean isMaxPlayer() {
        return maxPlayer;
    }

    public boolean isMinPlayer() {
        return minPlayer;
    }

    public int getGlobalCoins() {
        return globalCoins;
    }

    public void setGlobalCoins(int globalCoins) {
        this.globalCoins = globalCoins;
    }

    @SuppressWarnings("deprecation")
    public void start() {

        Location spawn = new Location(getServer().getWorlds().get(0), 0, 78, 0);

        for(Player player : server.getOnlinePlayers()) {
            player.teleport(spawn); // Teleport player to spawn
            player.setGameMode(GameMode.ADVENTURE); // Set player gamemode
        }

        // Timer before start

        for(Player player : server.getOnlinePlayers()) {
            player.sendTitle(ChatColor.GREEN + "3", "");
            player.playNote(player.getLocation(), Instrument.PIANO, new Note(0, Note.Tone.E, true));
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {

            for(Player player : server.getOnlinePlayers()) {
                player.sendTitle(ChatColor.YELLOW + "2", "");
                player.playNote(player.getLocation(), Instrument.PIANO, new Note(0, Note.Tone.E, true));
            }
        }, 20L);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {

            for(Player player : server.getOnlinePlayers()) {
                player.sendTitle(ChatColor.RED + "1", "");
                player.playNote(player.getLocation(), Instrument.PIANO, new Note(0, Note.Tone.E, true));
            }
        }, 40L);

        // Start
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {

            start = true; // Set start

            server.getOnlinePlayers().forEach(player -> player.playNote(player.getLocation(), Instrument.PIANO, new Note(18)));

        }, 60L);
    }

    public void end() {

        end = true; // Set end

        // Sort playerPacManList
        Collections.sort(playerPacManList);

        // Send end message
        int size = playerPacManList.size();
        server.broadcastMessage(ChatColor.GOLD + "------------------------------");
        if(size >= 1) {
            server.broadcastMessage(ChatColor.GREEN + "      Premier: " + playerPacManList.get(size - 1).getName() + ChatColor.GRAY + "(" + playerPacManList.get(size - 1).getCoins() + ")");
            if(size >= 2) {
                server.broadcastMessage(ChatColor.GREEN + "      Dexiéme: " + playerPacManList.get(size - 2).getName() + ChatColor.GRAY + "(" + playerPacManList.get(size - 2).getCoins() + ")");
                if(size >= 3)
                server.broadcastMessage(ChatColor.GREEN + "      Troisiéme: " + playerPacManList.get(size - 3).getName() + ChatColor.GRAY + "(" + playerPacManList.get(size - 3).getCoins() + ")");
            }
        }
        server.broadcastMessage(ChatColor.GOLD + "------------------------------");

        // Kick players and shutdown the server after 15.30s
        server.getScheduler().runTaskTimer(plugin, () -> {

            // TODO: Remove com
            //server.getOnlinePlayers().forEach(player -> player.kickPlayer(""));
            //server.shutdown();

        }, 310L, 0L);
    }
}
