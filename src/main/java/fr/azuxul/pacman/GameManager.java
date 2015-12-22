package fr.azuxul.pacman;

import fr.azuxul.pacman.player.PlayerPacMan;
import fr.azuxul.pacman.scoreboard.ScoreboardPacMan;
import fr.azuxul.pacman.timer.TimerPacMan;
import net.samagames.api.games.Game;
import net.samagames.api.games.themachine.messages.ITemplateManager;
import net.samagames.tools.powerups.PowerupManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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
public class GameManager extends Game<PlayerPacMan> {

    private final Server server;
    private final Logger logger;
    private final Plugin plugin;
    private final TimerPacMan timer;
    private final ScoreboardPacMan scoreboard;
    private final PowerupManager powerupManager;
    private int remainingGlobalCoins, globalCoins;

    /**
     * Class constructor
     *
     * @param logger          plugin logger
     * @param server          server
     * @param gameCodeName    unique id for game
     * @param gameName        game name
     * @param gameDescription game description
     * @param gamePlayerClass game players class
     */
    public GameManager(Logger logger, JavaPlugin plugin, Server server, String gameCodeName, String gameName, String gameDescription, Class<PlayerPacMan> gamePlayerClass) {

        super(gameCodeName, gameName, gameDescription, gamePlayerClass);

        this.server = server;
        this.logger = logger;
        this.plugin = plugin;
        this.scoreboard = new ScoreboardPacMan(ChatColor.YELLOW + "PacMan", this);
        this.timer = new TimerPacMan(this);
        this.powerupManager = new PowerupManager(plugin);
    }

    /**
     * Get list of playerPacMan
     *
     * @return playerListPacMan
     */
    public List<PlayerPacMan> getPlayerPacManList() {
        return new ArrayList<>(getInGamePlayers().values());
    }

    /**
     * Get game timer
     *
     * @return timer
     */
    public TimerPacMan getTimer() {
        return timer;
    }

    /**
     * Get game scoreboard
     *
     * @return scoreboard
     */
    public ScoreboardPacMan getScoreboard() {
        return scoreboard;
    }

    /**
     * Get server
     *
     * @return server
     */
    public Server getServer() {
        return server;
    }

    /**
     * Get logger of plugin
     *
     * @return logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Get plugin
     *
     * @return plugin
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Get powerup manager
     *
     * @return powerupManager
     */
    public PowerupManager getPowerupManager() {
        return powerupManager;
    }

    /**
     * Get number of global coins remaining
     *
     * @return remainingGlobalCoins
     */
    public int getRemainingGlobalCoins() {
        return remainingGlobalCoins;
    }

    /**
     * Set number of global coins remaining
     *
     * @param remainingGlobalCoins global coins remaining
     */
    public void setRemainingGlobalCoins(int remainingGlobalCoins) {
        this.remainingGlobalCoins = remainingGlobalCoins;
    }

    /**
     * Set number of global coins
     *
     * @param globalCoins global coins
     */
    public void setGlobalCoins(int globalCoins) {
        this.setRemainingGlobalCoins(globalCoins);
        this.globalCoins = globalCoins;
    }

    /**
     * Start the game
     */
    @SuppressWarnings("deprecation")
    @Override
    public void startGame() {

        super.startGame();

        Location spawn = new Location(getServer().getWorlds().get(0), 0, 78, 0);
        ItemStack woodenSword = new ItemStack(Material.WOOD_SWORD);
        ItemMeta swordMeta = woodenSword.getItemMeta();

        swordMeta.spigot().setUnbreakable(true);
        woodenSword.setItemMeta(swordMeta);

        for (PlayerPacMan playerPacMan : getPlayerPacManList()) {

            Player player = playerPacMan.getPlayerIfOnline();

            if (player != null) {
                player.teleport(spawn); // Teleport player to spawn
                player.setGameMode(GameMode.ADVENTURE); // Set player game mode
                player.getInventory().clear(); // Clear inventory
                player.getInventory().addItem(woodenSword); // Give wooden sword

                playerPacMan.setInvulnerableRespawn();
            }
        }

        powerupManager.start();
    }

    /**
     * Set end of game
     */
    public void end() {

        ITemplateManager templateManager = getCoherenceMachine().getTemplateManager();
        List<PlayerPacMan> playerPacManList = getPlayerPacManList();
        List<PlayerPacMan> winners = getWinners(playerPacManList);

        timer.setToZero(); // Set timer to zero
        powerupManager.stop();

        // Add coins to players
        for (PlayerPacMan playerPacMan : playerPacManList) {

            int percentOfCoins = 0;

            if (playerPacMan.getGameCoins() > 0 && globalCoins > 0) {
                percentOfCoins = playerPacMan.getGameCoins() * 100 / globalCoins; // Calculate percent of player coins
                int coins = percentOfCoins / 5; // Calculate coins for player

                playerPacMan.addCoins(coins, percentOfCoins + "% des piéces récupérer");
            }

            if (winners.contains(playerPacMan)) {
                if (winners.indexOf(playerPacMan) == 0) {
                    playerPacMan.addCoins(30, "Partie gagné");
                    playerPacMan.addStars(2, "Partie gagné");
                } else {
                    playerPacMan.addCoins(15, "Términé dans le classement");
                    playerPacMan.addStars(1, "Términé dans le classement");
                }
            }

            if (percentOfCoins >= 35) {
                playerPacMan.addStars(1, "Plus de 35% de coins récupéré");
            }
        }

        // Display winners
        if (!winners.isEmpty()) { // If winners is not empty

            int winnerSize = winners.size();

            if (winnerSize < 3) { // If winners size < 3

                PlayerPacMan winner = winners.get(0); // Get winner

                templateManager.getPlayerWinTemplate().execute(winner.getPlayerIfOnline(), winner.getGameCoins()); // Display player win template
            } else {

                PlayerPacMan winner = winners.get(2), second = winners.get(1), third = winners.get(0); // Get winners

                templateManager.getPlayerLeaderboardWinTemplate().execute(winner.getPlayerIfOnline(), second.getPlayerIfOnline(), third.getPlayerIfOnline(), winner.getGameCoins(), second.getGameCoins(), third.getGameCoins()); // Display players leadboard template
            }
        }

        this.handleGameEnd();
    }

    private List<PlayerPacMan> getWinners(List<PlayerPacMan> playerPacManList) {

        List<PlayerPacMan> winners = new ArrayList<>();

        // Sort playerPacManList
        Collections.sort(playerPacManList);

        int playerPacManListSize = playerPacManList.size();

        // Get winners
        for (int i = 1; i <= 3; i++) {
            if (winners.size() >= playerPacManListSize)
                break;

            PlayerPacMan playerPacMan = playerPacManList.get(playerPacManListSize - i);

            if (playerPacMan.getPlayerIfOnline() != null)
                winners.add(playerPacMan);

        }

        Collections.sort(winners);

        return winners;
    }
}
