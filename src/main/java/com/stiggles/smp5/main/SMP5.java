/**
 * Plugin for Stiggles SMP Season 5, the biggest,
 * most ambitious project yet.
 *
 * @author Kyler Stigelman
 * @version 0.0.0
 * @since 2022-08-28
 */
package com.stiggles.smp5.main;

import com.stiggles.smp5.commands.*;
import com.stiggles.smp5.entity.Entities;
import com.stiggles.smp5.entity.lostMerchant.InventoryManager;
import com.stiggles.smp5.entity.lostMerchant.LostMerchant;
import com.stiggles.smp5.entity.lostMerchant.MerchantListener;
import com.stiggles.smp5.entity.monsters.KillMagmaBoss;
import com.stiggles.smp5.entity.npc.*;
import com.stiggles.smp5.entity.npc.dialoguenpc.*;
import com.stiggles.smp5.entity.npc.shopnpcs.*;
import com.stiggles.smp5.items.Pickaxes;
import com.stiggles.smp5.items.Swords;
import com.stiggles.smp5.items.armor.AnarchysWardrobe;
import com.stiggles.smp5.items.armor.PeacesSymphony;
import com.stiggles.smp5.items.armor.runArmorCheck;
import com.stiggles.smp5.items.bows.BoomBow;
import com.stiggles.smp5.items.bows.GlowBow;
import com.stiggles.smp5.items.crafting.CustomCrafting;
import com.stiggles.smp5.listeners.*;
import com.stiggles.smp5.managers.BankManager;
import com.stiggles.smp5.managers.Bounty;
import com.stiggles.smp5.managers.MobKillListener;
import com.stiggles.smp5.player.StigglesPlayer;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

public class SMP5 extends JavaPlugin implements Listener {
    /*
     *  TODO - REMOVE NPC FROM plugin.yml
     */
    private static SMP5 instance;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public boolean inDungeon = true;
    public BankManager bankManager;
    //public PacketListener packetListener;
    public static SMP5 getInstance() {
        return instance;
    }
    Convergence c;
    private Database database;

    private PlayerManager playerManager;
    private ArrayList<UUID> registeredUUIDs;

    private ArrayList<StigglesNPC> npcs;
    public HashMap<String, StigglesPlayer> online_players;
    private ArrayList<String> toggled = new ArrayList<>();
    //private Plugin plugin = SMP5.getPlugin(SMP5.class);
    Random random = new Random(System.currentTimeMillis());
    LostMerchant merchant = new LostMerchant();
    InventoryManager inventoryManager = new InventoryManager();
    MerchantListener merchantListener = new MerchantListener(this);

    boolean open = false;

    @Override
    public void onEnable() {

        if (getServer ().getPluginManager ().getPlugin ("Citizens") == null || !getServer ().getPluginManager ().getPlugin ("Citizens").isEnabled()) {
            getLogger ().log (Level.SEVERE, "Citizens 2.0 not found or not enabled");
            getServer ().getPluginManager ().disablePlugin (this);
            return;
        }

        this.getConfig().options().copyDefaults();
        this.saveDefaultConfig();
        //Use CitizensAPI

        instance = this;
        database = new Database();

        try {
            database.connect ();
        }
        catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("NVTECH: Failed to connect to database.");
        }


        if (Bukkit.getWorld ("world") == null) {
            Bukkit.getConsoleSender().sendMessage("NVTECH: Could not load world.");
            Bukkit.getServer().shutdown();
        }
        if (Bukkit.getWorld ("sanctuary") == null) {
            new WorldCreator("sanctuary").createWorld();
        }


        registeredUUIDs = new ArrayList<>();
        online_players = new HashMap<>();
        playerManager = new PlayerManager();
        bankManager = new BankManager(this);
        Bounty.initializeMap(this);

        if (database.isConnected()) {
            //LOAD Registered player (UUIDS) from database
            try {
                ResultSet rs = database.query("SELECT * FROM player;");
                if (rs != null) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString(1));
                        registeredUUIDs.add(uuid);
                    }
                    rs.close();
                }
            } catch (SQLException e) {
                Bukkit.getConsoleSender().sendMessage("NVTECH: Failed to fetch players");
            }
        }
        Bukkit.getConsoleSender().sendMessage("Adding NPC");
        /*
            This is the lobby setup stuff
         */
        registerCommands();
        if (CitizensAPI.hasImplementation()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    registerEvents();
                    registerCommands();
                    createNPCs();
                }
            }.runTaskLater(this, 5);
        }
    }

    @Override
    public void onDisable() {
        instance = null;

        for (Player p : Bukkit.getOnlinePlayers())
            p.kickPlayer("Server is shutting down!");
        //Update world database
        BankManager.onDisable();
        //database.runQueue();
        try {
            database.connect();
            if (database.isConnected()) {
                database.runQueue();

                if (database != null)
                    database.disconnect();
            }
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("Failed to close db");
        }

        //bankManager.onDisable();

    }

    public DateTimeFormatter getFormatter() {return formatter; }
    public Database getDatabase() { return database; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public int getRandom () { return random.nextInt(); }
    public ArrayList<String> getToggledChatPlayers () { return toggled; }

    public static SMP5 getPlugin () {
        return instance;
    }

    public boolean isOpen () { return open; }
    public void setOpen (boolean b) { open = b; }
    @EventHandler
    public void onCitizensEnable(CitizensEnableEvent ev) {
        Bukkit.getConsoleSender().sendMessage("NV: Citizens Plugin enabled");
    }

    @EventHandler
    public void onPlayerJoin (PlayerJoinEvent e) {

        //Check if player is registered already
        Player p = e.getPlayer();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            int timeElapsed = 0;
            boolean inc = true;
            @Override
            public void run() {
                Bukkit.getConsoleSender().sendMessage("Spawning..." + timeElapsed);
                Bukkit.getWorld ("world").spawnParticle(Particle.VILLAGER_ANGRY,
                        new Location(p.getWorld(),
                                p.getLocation().getX() + Math.cos (Math.toRadians(timeElapsed * 10)),
                                p.getLocation().getY() + (timeElapsed * 0.025),
                                p.getLocation().getZ() + Math.sin (Math.toRadians(timeElapsed * 10))), 2, 0.1, 0.1, 0.1, null);
                if (inc) {
                    ++timeElapsed;
                    if (timeElapsed > 72)
                        inc = false;
                }
                else {
                    --timeElapsed;
                    if (timeElapsed <= 0)
                        inc = true;
                }
            }
        }, 0, 2);
        /*
        if (!registeredUUIDs.contains(p.getUniqueId())) {
            //Register player record

            database.execute("INSERT INTO player VALUES ('" + p.getUniqueId() + "', '" + p.getName() + "', " + 0 + ")");
            //Register bank record
            database.execute("INSERT INTO bank VALUES ('" + p.getUniqueId() + "', '" + 0 + ")");
        }*/
        //online_players.put (e.getPlayer ().getName (), new StigglesPlayer());
        //If so, add uuid + stigglesplayer to online_players
        //Otherwise, register new StigglesPlayer UUID and add to online_players


        //for (StigglesNPC n : NPCManager.getHashMap().values ())
          //  n.showToPlayer(e.getPlayer());

        //npc3.SetHolding(Material.TRIDENT);
        //npcs.get (2).SetHolding(Material.TRIDENT);
        //npcs.get (4).SetHolding (Material.GOLDEN_PICKAXE);
    }

    public void registerEvents () {
        PluginManager manager = Bukkit.getPluginManager();

        manager.registerEvents(new OnArmorStandInteract(), this);
        //manager.registerEvents(this, this);
        //manager.registerEvents (new ConnectionListener(this), this);
        //Bukkit.getPluginManager().registerEvents (packetListener, this);
        //Bukkit.getPluginManager().registerEvents (new NPCListener(this), this);
        manager.registerEvents(new CitizensRightClickEvent(this), this);
        manager.registerEvents(new ElytraEventListener(this), this);
        //manager.registerEvents(new DungeonListener(this), this);
        manager.registerEvents(new MobKillListener(this), this);
        manager.registerEvents(new NetherListener(), this);
        manager.registerEvents(new KillMagmaBoss(this), this);

        //Accent's plugin stuff
        runArmorCheck armorCheck = new runArmorCheck(this);
        CustomCrafting cc = new CustomCrafting(this);
        Bukkit.getPluginManager().registerEvents(new Swords(), this);
        Bukkit.getPluginManager().registerEvents(new BoomBow(), this);
        Bukkit.getPluginManager().registerEvents(new GlowBow(), this);
        Bukkit.getPluginManager().registerEvents(new Pickaxes(), this);
        Bukkit.getPluginManager().registerEvents(new AnarchysWardrobe(), this);
        Bukkit.getPluginManager().registerEvents(new PeacesSymphony(), this);
        Bukkit.getPluginManager().registerEvents(new Entities(), this);
        Bukkit.getPluginManager().registerEvents(new EndEyeListener(), this);
        //manager.registerEvents(this, this);

        try {
            database.connect();
            if (database.isConnected()) {
                manager.registerEvents(new LogEventListener(this), this);
                manager.registerEvents(new BountyListeners(this), this);
            }
        }
        catch (SQLException e) {

        }

    }

    /**
     * Loads all NPCs into the world and saves them as a StigglesNPC object.
     */
    public void createNPCs () {
        CitizensAPI.getNPCRegistry().deregisterAll();
        npcs = new ArrayList<>();
        //npcs.add (new Ned(this, "Ned", new Location(Bukkit.getWorld("world"), 0, 0, 0)));
        //c = new Convergence(new Location (Bukkit.getWorld ("world"), 0, 100, 0), 1);
        //particle dust 0.71 0.33 0.79 1 -1.90 71.00 -0.00 0.1 0.5 0.1 1 10

        npcs.add (new Starry (this, "Starry", new Location(Bukkit.getWorld("world"), -708.5, 67, -1110.5)));
        npcs.add (new EggDONTTake(this, "Francis Smurf", new Location(Bukkit.getWorld("world"), 82.5, 101, 755.5)));
        npcs.add (new DremBot (this, "Drem-Bot", new Location(Bukkit.getWorld("world"), 1154.5, 74, 127.5)));
        npcs.add (new DungeonKeeper(this, "Dungeon Keeper", new Location(Bukkit.getWorld("world"), 1135.5, 78, 156.5)));
        npcs.add (new Mister8Bit(this, "Luke the Fisherman", new Location(Bukkit.getWorld("world"), 774.5, 77, -596.5)));
        npcs.add (new Spiffy (this, "Spiffy", new Location(Bukkit.getWorld("world"), -709.5, 66, -1121)));
        npcs.add (new Astronomer(this, "The Astronomer", new Location(Bukkit.getWorld("world"), -900.5, 120, -1113.5)));
        npcs.add (new Inventor(this, "The Inventor", new Location(Bukkit.getWorld("world"), 1149.5, 74, 120.5)));
        npcs.add (new Philippe(this, "Sir Philippe Alfred", new Location(Bukkit.getWorld("world"), 1128.5, 71, 118.5)));
        npcs.add (new Baggins (this, "Mr. Orangeflips", new Location(Bukkit.getWorld("world"), 99.5, 92, 757.5)));
        npcs.add (new Drem (this, "Captain Beast", new Location(Bukkit.getWorld("world"), 1675.5, 107, -1133.5)));
        npcs.add (new Beachman (this, "Beach Man", new Location (Bukkit.getWorld("world"), -1480.5, 63, 1024.5)));
        npcs.add (new Chickens (this, "Gabe", new Location (Bukkit.getWorld("world"), 788.5, 83, -422.5)));
        npcs.add (new Bear (this, "BearSharken", new Location (Bukkit.getWorld("world"), 540.5, 92, -912.5)));
        npcs.add (new DrTrog (this, "Dr. Trog", new Location(Bukkit.getWorld ("world"), 1489.5, 136, -1475.5)));
        npcs.add (new Morabito (this, "Mr. Morabito", new Location(Bukkit.getWorld("world"), -751.5, 66,-1427.5)));
        npcs.add (new Mole (this, "Mole 'a Quacks", new Location(Bukkit.getWorld("world"), 71.5, 111, 784.5)));
        npcs.add (new Tiger (this, "Tigerfist", new Location (Bukkit.getWorld("world"), 45.5, 93, 818.5)));
        npcs.add (new Alejandro(this, "Alejandro", new Location (Bukkit.getWorld("world"), 1252.5, 98, 1487.5)));
        npcs.add (new Ralph (this, "Ralph", new Location(Bukkit.getWorld("world"), 1250.5, 93, 1492)));
        npcs.add (new MaskedStranger(this, "Masked Stranger", new Location(Bukkit.getWorld("world"), -772.5, 157, 1381.5)));
        npcs.add (new Scubadiver(this, "Scuba Diver", new Location(Bukkit.getWorld("world"), 1505.5, 73, -1279.5)));
        npcs.add(new Shrek(this, "Shrek", new Location(Bukkit.getWorld("world"), 739.5, 66, 1162.5)));
        npcs.add(new MindlessGuy(this, "Mindless Guy", new Location(Bukkit.getWorld("world"), 28.5, 91, 855.5)));
        npcs.add(new NetherWizard(this, "Wondrous Wizard", new Location(Bukkit.getWorld("world"), -976.5, 67, -278.5)));

        npcs.add (new Nouveau(this, "Nouveau", new Location (Bukkit.getWorld("sanctuary"), 8.5, -59, 8.5)));
        //Nouveau 52, 132, 746
    }
    public void registerCommands () {
        //Bukkit.getPluginCommand("coins").setExecutor(new CoinCommand());
        //saveDefaultConfig()
        //stats = new PluginFile(this, "stats.yml", "stats.yml");
        //Load important database variables
        Bukkit.getPluginCommand ("loadcitizens").setExecutor (new NPCCommand (this));
        Bukkit.getPluginCommand ("world").setExecutor (new ChangeWorldCommand ());
        Bukkit.getPluginCommand("o").setExecutor(new OpenWorldCommand(this));
        //Bukkit.getPluginCommand("start-dungeon").setExecutor (new DungeonStartCommand());
        Bukkit.getPluginCommand("coins").setExecutor(new CoinCommand());
        Bukkit.getPluginCommand("togglecoin").setExecutor(new ToggleCoinChat(this));
        Bukkit.getPluginCommand("smm").setExecutor(new SendMultiMessage (this));
        Bukkit.getPluginCommand("get-items").setExecutor(new GetItems());
    }

    public static int rollNumber(int min, int max){
        Random rand = new Random();
        int randomNumber = rand.nextInt(max - min + 1) + min;

        return randomNumber;
    }

    @EventHandler
    public void interact(PlayerInteractEvent event){

        Location potLocation = new Location(Bukkit.getWorld("world"), -147, 46, 864);

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
            if (event.getClickedBlock().getType().equals(Material.DECORATED_POT) && event.getClickedBlock().getLocation().equals(potLocation)){
                ItemStack wheel = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) wheel.getItemMeta();
                skullMeta.setDisplayName(ChatColor.WHITE + "Scubas Wheel");
                skullMeta.setLore(Arrays.asList(ChatColor.BLUE + "Quest Item", ChatColor.GRAY + ChatColor.ITALIC.toString() + "A wheel that was once on a ship."));
                skullMeta.setLocalizedName("scuba_ship_wheel");

                PlayerProfile p = Bukkit.createPlayerProfile(UUID.randomUUID());
                try {
                    PlayerTextures pt = p.getTextures();
                    pt.setSkin(new URL("https://textures.minecraft.net/texture/6b60171a946b630f46b7b626f4a780bd1a2de9ae3b2bc8a67b6f5bd970eb7"
                    ));
                    p.setTextures(pt);
                }
                catch (MalformedURLException e) {

                }
                skullMeta.setOwnerProfile(p);
                wheel.setItemMeta(skullMeta);
                ItemStack pendant = new ItemStack(Material.HEART_OF_THE_SEA);
                ItemMeta pendantMeta = pendant.getItemMeta();
                pendantMeta.setDisplayName(ChatColor.AQUA+"The Friend's Pendant");
                pendantMeta.setLore(Arrays.asList(ChatColor.BLUE + "Quest Item", ChatColor.GRAY + ChatColor.ITALIC.toString() + "Once was the center of a great friendship,",
                        ChatColor.GRAY + ChatColor.ITALIC.toString() + "now it's just a relic of a memory..."));
                pendantMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                pendantMeta.setLocalizedName("the_friends_pendant");

                pendant.setItemMeta(pendantMeta);
                if (event.getPlayer().getInventory().contains(pendant)){
                    event.getPlayer().getInventory().addItem(wheel);
                }
            }
        }
    }

}
