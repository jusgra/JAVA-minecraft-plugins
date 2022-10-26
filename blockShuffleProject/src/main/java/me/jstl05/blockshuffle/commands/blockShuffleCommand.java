package me.jstl05.blockshuffle.commands;

import me.jstl05.blockshuffle.BlockShuffle;
import me.jstl05.blockshuffle.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class blockShuffleCommand implements CommandExecutor, Listener {

    //game config
    private final int ROUND_TIME = 300; //seconds
    private final int[] TIME_NOTIFICATIONS_AT = {300, 240, 180, 120, 60, 30, 15, 10, 5, 4, 3, 2, 1};
    private final float  BLOCK_BELOW_THRESHOLD = 0.5f;
    private final String[] EXCLUDE_BLOCKS = {"DIAMOND", "EMERALD", "GOLD", "LAZULI", "PURPUR", "WALL",
            "CRACKED", "MOSSY", "EXPOSED", "WEATHERED", "OXIDIZED", "SPONGE", "RESPAWN", "FENCE",
            "CRYING", "BLACKSTONE", "SEA", "MYCELIUM", "REINFORCED", "GILDED", "BASALT", "NYLIUM",
            "PODZOL", "RAW", "HYPHAE", "CORAL", "NETHERITE", "NETHER WART", "MELON", "END", "SIGN",
            "TARGET", "BANNER", "SHROOMLIGHT", "ANCIENT", "CAKE", "SHULKER", "TINTED", "DRAGON_EGG",
            "LAPIS", "COMMAND_BLOCK", "SCULK", "CHIPPED", "DAMAGED", "PRISMARINE", "MOVING_PISTON",
            "PISTON_HEAD", "QUARTZ", "TURTLE", "LODESTONE", "WARPED", "TRAPDOOR", "JIGSAW", "CALCITE",
            "TUFF", "COARSE_DIRT", "ROOTED DIRT", "MUD", "MANGROVE", "AMETHYST", "WAXED", "SPAWNER",
            "CARVED_PUMPKIN", "JACK_O_LANTERN", "ENCHANTING", "BEACON", "BARRIER", "CONDUIT", "SLIME",
            "HONEY", "PRESSURE_PLATE", "HONEYCOMB_BLOCK", "SOUL_LANTERN", "SOUL_CAMPFIRE", "FROGLIGHT",
            "FROSTED_ICE", "INFESTED", "ROOTED_DIRT", "DEEPSLATE", "CHAIN", "STRUCTURE_BLOCK", "DAYLIGHT",
            "REDSTONE_LAMP"};

    private boolean gameInProgress = false;
    private boolean skipInsist = false;
    private GamePlayer[] GamePlayers;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(args.length==0) return false;

        switch (args[0].toLowerCase()) {
            case "start" :
                if(gameInProgress) sender.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "Game has already started !");
                else {
                    if(Bukkit.getOnlinePlayers().size()==0) {
                        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "There are no players online !");
                        return true;
                    }
                    gameInProgress = true;
                    startGameTimer();
                    putOnlinePlayerToArray();
                }
                return true;


            case "stop" :
                if(gameInProgress){
                    Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "Game was stopped !");
                    gameInProgress = false;
                } else sender.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "Game is not running !");
                return true;


            case "skip" :
                if(gameInProgress){
                    Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "Block was skipped !");
                    skipInsist = true;
                } else sender.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "Game is not running !");
                return true;
        }

        return false;
    }

    public void startGameTimer(){
        new BukkitRunnable() {
            int time = ROUND_TIME;

            @Override
            public void run() {
                if(time != 0 && gameInProgress) {
                    //everyone found their blocks, nextRound (during the Game)
                    if(checkIfAllPlayersFoundTheirBlock() || skipInsist) {
                        nextRoundPlease();
                        time = ROUND_TIME;
                    }
                    else {
                        broadcastTimeNotifications(time);
                        time--;
                    }
                }
                //someone did not find their block, game over
                else if(gameInProgress && !checkIfAllPlayersFoundTheirBlock()){
                    gameInProgress=false;
                    checkWhoLost();
                    cancel();
                //everyone found their blocks, nextRound (on the Last second)
                } else if(checkIfAllPlayersFoundTheirBlock() || skipInsist) {
                    nextRoundPlease();
                    time = ROUND_TIME;
                //stop command execution
                } else {
                    //System.out.println("INSIDE fifth ELSE");
                    cancel();
                }
            }
        }.runTaskTimer(BlockShuffle.getPlugin(), 0, 20);
    }

    //Used for Testing
    public void runTestCycle(){
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                Material[] material = Material.values();
                if(material[i].isBlock()&&material[i].isSolid()&&i<material.length-1) {
                    if(!isBlockOutsideExcludeList(material[i].name())) {
                        System.out.println("Index - " + i + " Material - " + material[i]);
                    }
                }
                i++;

            }
        }.runTaskTimer(BlockShuffle.getPlugin(), 0, 1);
    }

    public void checkWhoLost() {
        for (GamePlayer player : GamePlayers) {
            if (!player.foundTheBlock)
                Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD +
                        player.onePlayer.getName() + " failed to found his block!");
        }
    }

    public void nextRoundPlease() {
        for (GamePlayer player : GamePlayers) {
            player.setNewBlock(getRandomBlock());
            player.foundTheBlock = false;
        }
        newBlockAssignedMessageToPlayers();
        skipInsist = false;
    }

    public void newBlockAssignedMessageToPlayers() {
        for (GamePlayer gamepl : GamePlayers) {
            gamepl.onePlayer.sendMessage(ChatColor.GREEN + "You have to find - " + ChatColor.BOLD +
                    gamepl.assignedMaterial.toString().replace("_", " ") + ChatColor.RESET
                    + ChatColor.GREEN + " and stand on it !");
        }
    }

    public void putOnlinePlayerToArray(){
        GamePlayers = new GamePlayer[Bukkit.getOnlinePlayers().size()];
        int index = 0;
        for (Player player: Bukkit.getOnlinePlayers()) {
            GamePlayer singleGamer = new GamePlayer(player, getRandomBlock());
            GamePlayers[index] = singleGamer;
            index++;
        }
        newBlockAssignedMessageToPlayers();
    }

    public boolean checkIfAllPlayersFoundTheirBlock () {
        int numberOfPlayersFound = 0;
        for (GamePlayer player: GamePlayers) {
            if(player.foundTheBlock)  numberOfPlayersFound++;
        }
        return numberOfPlayersFound == GamePlayers.length;
    }

    public void broadcastTimeNotifications (int time) {
        for (int singleCheck : TIME_NOTIFICATIONS_AT){
            if(time==singleCheck) {
                if(time>60) Bukkit.broadcastMessage(ChatColor.RED + "" + time/60 + " minutes left !");
                else if (time==60) Bukkit.broadcastMessage(ChatColor.RED + "1 minute left !");
                else if (time>1) Bukkit.broadcastMessage(ChatColor.RED + "" + time + " seconds left to found your block !");
                else Bukkit.broadcastMessage(ChatColor.RED + "1 second left to found your block !");
            }
        }
    }

    public Material getRandomBlock() {

        Random rng = new Random();
        Material[] materialsArray = Material.values();
        Material rngMaterial = null;
        while(true) {
            rngMaterial = materialsArray[rng.nextInt(materialsArray.length)];
            if(rngMaterial.isBlock() && rngMaterial.isSolid()) {
                if(isBlockOutsideExcludeList(rngMaterial.name())) break;
            }
        }
        return rngMaterial;
    }

    public boolean isBlockOutsideExcludeList(String material) {
        for (String singleExcludeBlock : EXCLUDE_BLOCKS) {
            if(material.contains(singleExcludeBlock)) return false;
        }
        return true;
    }

    public void checkIfPlayerFoundTheirBlock (Player movingPlayer) {
        for (GamePlayer player : GamePlayers) {
            if(player.onePlayer.getUniqueId() == movingPlayer.getUniqueId() && !player.foundTheBlock) {
                Material blockBelow = checkTheBlockBeneath(movingPlayer);
                if(blockBelow==player.assignedMaterial) someoneFoundTheirBlock(player);
            }
        }
    }

    public Material checkTheBlockBeneath(Player player){
        Location location = player.getLocation();
        location.setY(location.getY()-BLOCK_BELOW_THRESHOLD);
        return location.getBlock().getType();
    }

    public void someoneFoundTheirBlock (GamePlayer player) {
        player.foundTheBlock=true;
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + player.onePlayer.getName() + " found his block !");
    }

    @EventHandler
    public void onPlayerMove (PlayerMoveEvent e) {
        if(gameInProgress) checkIfPlayerFoundTheirBlock(e.getPlayer());
    }
}
