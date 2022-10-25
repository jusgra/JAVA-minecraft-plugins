package me.jstl05.blockshuffle.commands;

import me.jstl05.blockshuffle.BlockShuffle;
import me.jstl05.blockshuffle.gamePlayer;
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

    //game settings
    private final int roundTime = 300;
    private final int[] timeNotificationsAt = {300, 240, 180, 120, 60, 30, 15, 10, 5, 4, 3, 2, 1};
    private final String[] excludeBlocks = {"DIAMOND", "EMERALD", "GOLD", "LAZULI", "PURPUR",
            "CRACKED", "MOSSY", "EXPOSED", "WEATHERED", "OXIDIZED", "SPONGE", "RESPAWN",
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
    private gamePlayer[] gamePlayers;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(args.length==0) return false;

        switch (args[0].toLowerCase()) {
            case "start" :
                if(Bukkit.getOnlinePlayers().size()==0) {
                    sender.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "There are no players online !");
                    return true;
                }
                gameInProgress = true;
                startGame();
                putOnlinePlayerToArray();
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

    public void startGame(){
        new BukkitRunnable() {
            int time = roundTime;

            @Override
            public void run() {
                if(time != 0 && gameInProgress) {
                    if(checkIfAllPlayersFoundTheirBlock() || skipInsist) {
                        nextRoundPlease();
                        time = roundTime;
                        //System.out.println("INSIDE second IF");
                    }
                    else {
                        broadcastTimeNotifications(time);
                        //Bukkit.broadcastMessage(ChatColor.RED + "timer - " + time);
                        time--;
                    }
                }
                else if(gameInProgress && !checkIfAllPlayersFoundTheirBlock()){
                    //System.out.println("INSIDE third ELSE");
                    gameInProgress=false;
                    checkWhoLost();
                    cancel();
                } else if(checkIfAllPlayersFoundTheirBlock() || skipInsist) {
                    //System.out.println("INSIDE fourth ELSE");
                    nextRoundPlease();
                    time = roundTime;
                } else {
                    //System.out.println("INSIDE fifth ELSE");
                    cancel();
                }
            }
        }.runTaskTimer(BlockShuffle.getPlugin(), 0, 20);
    }

    public void runTestCycle(){
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                Material[] material = Material.values();
                if(material[i].isBlock()&&material[i].isSolid()&&i<material.length-1) {
                    if(!checkIfBlockContainsExcludeList(material[i].name())) {
                        System.out.println("Index - " + i + " Material - " + material[i]);
                    }
                }
                i++;

            }
        }.runTaskTimer(BlockShuffle.getPlugin(), 0, 1);
    }

    public void checkWhoLost() {
//        StringBuilder finalMessage = new StringBuilder(100);
//        finalMessage.append(ChatColor.DARK_AQUA + "Losers of the game - ");
        for (gamePlayer player : gamePlayers) {
            if (!player.foundTheBlock)  Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + player.onePlayer.getName() + " failed to found his block!");
                //finalMessage.append(ChatColor.BOLD + player.onePlayer.getName() + ", ");
        }
//        Bukkit.broadcastMessage(finalMessage.toString());
    }

    public void nextRoundPlease() {
        for (gamePlayer player : gamePlayers) {
            player.setNewBlock(getRandomBlock());
            player.foundTheBlock = false;
        }
        messageToPlayers();
        System.out.println("NewBlock Assignation");
        skipInsist = false;
    }

    public void messageToPlayers() {
        for (gamePlayer gamep : gamePlayers) {
            gamep.onePlayer.sendMessage(ChatColor.GREEN + "You have to find - " + ChatColor.BOLD +
                    gamep.assignedMaterial.toString().replace("_", " ") + ChatColor.RESET
                    + ChatColor.GREEN + " and stand on it !");
        }
    }

    public void putOnlinePlayerToArray(){
        gamePlayers = new gamePlayer[Bukkit.getOnlinePlayers().size()];
        int index = 0;
        for (Player player: Bukkit.getOnlinePlayers()) {
            gamePlayer singleGamer = new gamePlayer(player, getRandomBlock());
            gamePlayers[index] = singleGamer;
            index++;
        }
        messageToPlayers();
    }

    public boolean checkIfAllPlayersFoundTheirBlock () {
        int numberFound = 0;
        for (gamePlayer player: gamePlayers) {
            if(player.foundTheBlock)  numberFound++;
        }
        return numberFound == gamePlayers.length;
    }

    public void broadcastTimeNotifications (int time) {
        for (int singleCheck : timeNotificationsAt){
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
                if(!checkIfBlockContainsExcludeList(rngMaterial.name())) break;
            }
        }
        return rngMaterial;
    }

    public boolean checkIfBlockContainsExcludeList(String current) {
        for (String singleExcludeBlock : excludeBlocks) {
            if(current.contains(singleExcludeBlock)) return true;
        }
        return false;
    }

    public void checkIfPlayerFoundTheirBlock (Player movingPlayer) {
        for (gamePlayer gP : gamePlayers) {
            if(gP.onePlayer.getUniqueId() == movingPlayer.getUniqueId() && !gP.foundTheBlock) {
                Material blockBelow = checkTheBlockBeneath(movingPlayer);
                if(blockBelow==gP.assignedMaterial) someoneFoundTheirBlock(gP);
            }
        }
    }

    public Material checkTheBlockBeneath(Player player){
        Location loc = player.getLocation();
        loc.setY(loc.getY()-0.5);
        return loc.getBlock().getType();
    }

    public void someoneFoundTheirBlock (gamePlayer player) {
        player.foundTheBlock=true;
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + player.onePlayer.getName() + " found his block !");
    }

    @EventHandler
    public void onPlayerMove (PlayerMoveEvent e) {
        if(gameInProgress) checkIfPlayerFoundTheirBlock(e.getPlayer());
    }
}
