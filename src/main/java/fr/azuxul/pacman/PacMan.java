package fr.azuxul.pacman;

import com.google.gson.JsonObject;
import fr.azuxul.pacman.entity.Gomme;
import fr.azuxul.pacman.event.PlayerEvent;
import fr.azuxul.pacman.powerup.BasicPowerup;
import fr.azuxul.pacman.powerup.PowerupBlindness;
import fr.azuxul.pacman.powerup.PowerupEffectType;
import fr.azuxul.pacman.powerup.PowerupSwap;
import net.minecraft.server.v1_9_R2.Entity;
import net.minecraft.server.v1_9_R2.EntityTypes;
import net.minecraft.server.v1_9_R2.World;
import net.samagames.api.SamaGamesAPI;
import net.samagames.tools.powerups.PowerupManager;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

/*
 * This file is part of PacMan.
 *
 * PacMan is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PacMan is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PacMan.  If not, see <http://www.gnu.org/licenses/>.
 */
public class PacMan extends JavaPlugin {

    private static GameManager gameManager;
    private Material gommeMaterial;
    private Material powerupMaterial;
    private String[] initRadius;

    public static GameManager getGameManager() {
        return gameManager;
    }

    /**
     * Initialize powerup manager
     * Registry powerups
     * Set spawn frequency
     */
    private static void powerupInitialisation() {

        PowerupManager powerupManager = gameManager.getPowerupManager();
        int spawnFrequency = SamaGamesAPI.get().getGameManager().getGameProperties().getConfigs().get("powerup-frequency").getAsInt();

        JsonObject jsonObject = SamaGamesAPI.get().getGameManager().getGameProperties().getConfigs().get("powerup-chance").getAsJsonObject();

        // Register powerups
        powerupManager.registerPowerup(new BasicPowerup(PowerupEffectType.SPEED, "speed", jsonObject));
        powerupManager.registerPowerup(new BasicPowerup(PowerupEffectType.JUMP_BOOST, "jump-boost", jsonObject));
        powerupManager.registerPowerup(new BasicPowerup(PowerupEffectType.DOUBLE_GOMMES, "double-gommes", jsonObject));
        powerupManager.registerPowerup(new BasicPowerup(PowerupEffectType.GOMME_MAGNET, "gommes-magnet", jsonObject));
        powerupManager.registerPowerup(new PowerupSwap(jsonObject));
        powerupManager.registerPowerup(new PowerupBlindness(jsonObject));

        powerupManager.setInverseFrequency(spawnFrequency); // Set spawn frequency
    }

    private static void registerEntityInEntityEnum(Class paramClass, String paramString, int paramInt) throws NoSuchFieldException, IllegalAccessException {
        ((Map<String, Class<? extends Entity>>) getPrivateStatic(EntityTypes.class, "c")).put(paramString, paramClass);
        ((Map<Class<? extends Entity>, String>) getPrivateStatic(EntityTypes.class, "d")).put(paramClass, paramString);
        ((Map<Integer, Class<? extends Entity>>) getPrivateStatic(EntityTypes.class, "e")).put(paramInt, paramClass);
        ((Map<Class<? extends Entity>, Integer>) getPrivateStatic(EntityTypes.class, "f")).put(paramClass, paramInt);
        ((Map<String, Integer>) getPrivateStatic(EntityTypes.class, "g")).put(paramString, paramInt);
    }

    private static Object getPrivateStatic(Class clazz, String f) throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(f);
        field.setAccessible(true);

        return field.get(null);
    }

    private void mapPreInitialisation() {

        Server server = gameManager.getServer();

        JsonObject json = SamaGamesAPI.get().getGameManager().getGameProperties().getConfigs().get("map-init").getAsJsonObject();

        initRadius = json.get("radius").getAsString().split(", ");

        if (initRadius.length > 3) {
            server.getLogger().warning("Map initialisation warning, radius is not valid !");
            return;
        }

        try {
            gommeMaterial = Material.getMaterial(json.get("gomme-block").getAsString().toUpperCase());
            powerupMaterial = Material.getMaterial(json.get("powerup-block").getAsString().toUpperCase());
        } catch (Exception e) {
            server.getLogger().warning("Map initialisation warning, blocks is not valid ! " + e);
            return;
        }

        mapInitialisation();
    }

    /**
     * Initialise the map :
     * Replace gold block with gommes
     */
    private void mapInitialisation() {

        Location baseLocation = gameManager.getMapCenter();
        if (baseLocation == null)
            return;
        org.bukkit.World world = baseLocation.getWorld();
        World worldNMS = ((CraftWorld) world).getHandle();

        // Replace gold block with gommes
        int globalGommes = 0;

        int xRadius = Integer.parseInt(initRadius[0]);
        int yRadius = Integer.parseInt(initRadius[1]);
        int zRadius = Integer.parseInt(initRadius[2]);

        int xMin = baseLocation.getBlockX() - xRadius;
        int xMax = baseLocation.getBlockX() + xRadius;
        int yMin = baseLocation.getBlockY() - yRadius;
        int yMax = baseLocation.getBlockY() + yRadius;
        int zMin = baseLocation.getBlockZ() - zRadius;
        int zMax = baseLocation.getBlockZ() + zRadius;

        if (yMin <= 0)
            yMin = 1;
        if (yMax > 255)
            yMax = 255;

        for (int x = xMin; x <= xMax; x++)
            for (int z = zMin; z <= zMax; z++)
                for (int y = yMin; y <= yMax; y++) {
                    globalGommes += registerBlock(x, y, z, world, worldNMS);
                }

        gameManager.getGommeManager().setGlobalGommes(globalGommes); // Set global gommes
        Collections.shuffle(gameManager.getGommeManager().getGommeList());
    }

    private int registerBlock(int x, int y, int z, org.bukkit.World world, World worldNMS) {

        PowerupManager powerupManager = gameManager.getPowerupManager();
        GommeManager gommeManager = gameManager.getGommeManager();

        Block block = world.getBlockAt(x, y, z); // Get block

        if (block.getType().equals(gommeMaterial)) { // If is gold block
            block.setType(Material.AIR); // Set air

            // Spawn normal gomme
            gommeManager.spawnGomme(worldNMS, x + 0.5, y - 0.3, z + 0.5, false);
            return 1;

        } else if (block.getType().equals(powerupMaterial)) {
            block.setType(Material.AIR); // Set air

            powerupManager.registerLocation(new Location(world, x + 0.5, y, z + 0.5)); // Register booster location
        }

        return 0;
    }

    @Override
    public void onEnable() {

        SamaGamesAPI samaGamesAPI = SamaGamesAPI.get();

        synchronized (this) {
            gameManager = new GameManager(this); // Register GameManager
        }

        samaGamesAPI.getGameManager().registerGame(gameManager); // Register game on SamaGameAPI
        samaGamesAPI.getGameManager().getGameProperties(); // Get properties
        samaGamesAPI.getGameManager().setLegacyPvP(true); // Get legacy pvp

        // Register events
        getServer().getPluginManager().registerEvents(new PlayerEvent(gameManager), this);

        // Register timer
        getServer().getScheduler().scheduleSyncRepeatingTask(this, gameManager.getTimer(), 0L, 20L);

        // Register entity
        try {
            registerEntityInEntityEnum(Gomme.class, "Gomme", 69);
        } catch (Exception e) {
            getLogger().warning("Error to register entity Gomme " + e);
        }

        // Kick players
        getServer().getOnlinePlayers().forEach(player -> player.kickPlayer(""));

        Location spawn = gameManager.getSpawn();
        org.bukkit.World world = spawn.getWorld();

        world.setSpawnLocation(spawn.getBlockX(), spawn.getBlockY() + 3, spawn.getBlockZ()); // Set spawn location
        world.setDifficulty(Difficulty.EASY); // Set difficulty
        world.setGameRuleValue("doMobSpawning", "false"); // Set doMobSpawning game rule
        world.setGameRuleValue("keepInventory", "true"); // Set keepInventory game rule
        world.setGameRuleValue("reducedDebugInfo", "true"); // Reduce debug info (Mask location)
        world.setStorm(false); // Clear storm
        world.setThundering(false); // Clear weather
        world.setThunderDuration(0); // Clear weather
        world.setWeatherDuration(0); // Clear weather

        powerupInitialisation();
        mapPreInitialisation();
    }

    @Override
    public void onDisable() {
        gameManager.getGommeManager().killAllGommes();
    }
}
