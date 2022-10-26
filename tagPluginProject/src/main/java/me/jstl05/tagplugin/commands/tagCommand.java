package me.jstl05.tagplugin.commands;

import me.jstl05.tagplugin.TagPlugin;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class tagCommand implements CommandExecutor, Listener {

    private Player hunter, runner;
    private boolean gameInProgress = false;
    private boolean gameLoading = false;

    //game config                                   //optimal values
    private final int PLAY_TIME = 120;              //120
    private final int COORDINATES_REACH = 5000;     //5000
    private final int BORDER_SIZE = 80;             //80
    private final int FREEZE_TIME = 5;              //5
    private final int[] TIME_NOTIFICATIONS_AT = {300, 240, 180, 120, 60, 30, 15, 10, 5, 4, 3, 2, 1};

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if(args.length < 1  || args.length > 2) return false;

        if(args[0].equals("stop")) {
            if(gameInProgress){
                Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "Game was stopped !");
                gameInProgress = false;
                hunter.getWorld().getWorldBorder().reset();
            } else if (gameLoading) sender.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "You cannot stop the game while its loading !");
            else sender.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "Game is not running !");
            return true;
        }

        if(args.length == 1) return false;

        if(isSelectedPlayersAvailable(args[0], args[1])) {
            if(!gameInProgress && !gameLoading) {

                Bukkit.broadcastMessage("GAME STARTED !!!");
                gameLoading = true;
                Location center = teleportToRandomLocation(hunter, runner);
                makeSpectators(center);
                setupPlayer(hunter);
                setupPlayer(runner);
                startLoadingTimer();

            } else sender.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "Game has already started, to stop current game, use /tag stop");
        } else sender.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "Game cannot start with these players, check provided players !");

        return true;
    }

    public void startGameTimer() {
        new BukkitRunnable() {
            int time = PLAY_TIME;

            @Override
            public void run() {
                if(time != 0 && gameInProgress) {
                    broadcastTimeNotifications(time);
                    time--;
                }
                else {
                    cancel();
                    if(gameInProgress) stopTheGame(runner);
                }
            }
        }.runTaskTimer(TagPlugin.getPlugin(), 0, 20);
    }

    public void startLoadingTimer() {
        new BukkitRunnable() {
            int time = FREEZE_TIME;

            @Override
            public void run() {
                if(time != 0) {
                    Bukkit.broadcastMessage(ChatColor.BLUE + "Game starts in "+ time);
                    time--;
                }
                else {
                    gameLoading = false;
                    cancel();
                    startTheGame();
                }
            }
        }.runTaskTimer(TagPlugin.getPlugin(), 0, 20);
    }

    public void startTheGame() {
        gameInProgress = true;
        if(!gameLoading) {
            hunter.sendMessage(ChatColor.DARK_GRAY + "You are a Hunter, use compass and tag " + ChatColor.BOLD + runner.getName() + " !");
            runner.sendMessage(ChatColor.DARK_GREEN + "You are a Runner, hide from the " + ChatColor.BOLD + hunter.getName() + " !");
            startGameTimer();
        }
    }

    public void stopTheGame(Player winner) {

        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "" + winner.getName() + ChatColor.YELLOW + " is a winner!");

        winner.getWorld().getWorldBorder().reset();
        gameInProgress = false;

        hunter = null;
        runner = null;
    }

    public void makeSpectators(Location center){
        for (Player p:Bukkit.getOnlinePlayers()) {
            if(p.getUniqueId() != hunter.getUniqueId() && p.getUniqueId() != runner.getUniqueId()){
                p.setGameMode(GameMode.SPECTATOR);

                Location spectatorSpawn = new Location(p.getWorld(), center.getX(), center.getY()+20, center.getZ());
                p.teleport(spectatorSpawn);
            }
        }
    }

    //unused currently, thinking of a better implementation
    public void removeSpectators(){
        for (Player p:Bukkit.getOnlinePlayers()) {
            if(p.getUniqueId() != hunter.getUniqueId() && p.getUniqueId() != runner.getUniqueId()){
                p.setGameMode(GameMode.SURVIVAL);

                //Location rng = new Location(hunter.getWorld(), runnerX+0.5, hunter.getWorld().getHighestBlockYAt(runnerX,runnerZ)+1, runnerZ+0.5);
                Location rng = p.getLocation();
                rng.setY(p.getWorld().getHighestBlockYAt((int) rng.getX(), (int) rng.getZ())+1);

                //Location spectatorSpawn = new Location(p.getWorld(), center.getX(), center.getY()+20, center.getZ());
                p.teleport(rng);
            }
        }
    }

    public void setupPlayer(Player p){

        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setGameMode(GameMode.SURVIVAL);

        p.getInventory().clear();
        p.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));
        p.getInventory().addItem(new ItemStack(Material.DIAMOND_AXE));
        p.getInventory().addItem(new ItemStack(Material.DIAMOND_SHOVEL));
        p.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 16));

        if(p.getUniqueId() == hunter.getUniqueId()) hunter.getInventory().addItem(new ItemStack(Material.COMPASS));

    }

    public void broadcastTimeNotifications(int time){
        for (int singleCheck : TIME_NOTIFICATIONS_AT){
            if(time==singleCheck) {
                if(time>60) Bukkit.broadcastMessage(ChatColor.RED + "" + time/60 + " minutes left !");
                else if (time==60) Bukkit.broadcastMessage(ChatColor.RED + "1 minute left !");
                else if (time>1) Bukkit.broadcastMessage(ChatColor.RED + "" + time + " seconds left !");
                else Bukkit.broadcastMessage(ChatColor.RED + "1 second left !");
            }
        }
    }

    public boolean isSelectedPlayersAvailable(String arg1, String arg2) {

        if(arg1==null || arg2 == null) return false;

        hunter = Bukkit.getPlayerExact(arg1);
        runner = Bukkit.getPlayerExact(arg2);

        if(hunter == null || runner == null) return false;
        else if (hunter.isDead() || runner.isDead()) return false;
        else if (hunter.getUniqueId() == runner.getUniqueId()) return false;
        else return true;
    }

    public Location teleportToRandomLocation(Player hunter, Player runner) {

        Random rng = new Random();

        Location hunterLocation = null;
        Location runnerLocation = null;

        Location randomCenterLoc = null;

        while(true) {
            int cordX = rng.nextInt(COORDINATES_REACH + COORDINATES_REACH) - COORDINATES_REACH;
            int cordZ = rng.nextInt(COORDINATES_REACH + COORDINATES_REACH) - COORDINATES_REACH;

            randomCenterLoc = new Location(hunter.getWorld(), cordX, hunter.getWorld().getHighestBlockYAt(cordX,cordZ)+1, cordZ);

            int offset = BORDER_SIZE /2 - 3;
            int hunterX = cordX + offset;
            int hunterZ = cordZ - offset;

            int runnerX = cordX - offset;
            int runnerZ = cordZ + offset;

            hunterLocation = new Location(hunter.getWorld(), hunterX+0.5, hunter.getWorld().getHighestBlockYAt(hunterX,hunterZ)+1, hunterZ+0.5);
            runnerLocation = new Location(hunter.getWorld(), runnerX+0.5, runner.getWorld().getHighestBlockYAt(runnerX,runnerZ)+1, runnerZ+0.5);

            Location checkWaterHunter = hunterLocation.clone();
            Location checkWaterRunner = runnerLocation.clone();

            checkWaterHunter.setY(checkWaterHunter.getY()-1);
            checkWaterRunner.setY(checkWaterRunner.getY()-1);

            if(checkWaterHunter.getBlock().getType() != Material.WATER
                    && checkWaterRunner.getBlock().getType()!=Material.WATER)
                break;
        }

        hunter.getWorld().getWorldBorder().setCenter(randomCenterLoc);
        hunter.getWorld().getWorldBorder().setSize(BORDER_SIZE);

        System.out.println("Hunter loc - " + hunterLocation.getY());
        System.out.println("Runner loc - " + runnerLocation.getY());

        hunter.teleport(hunterLocation);
        runner.teleport(runnerLocation);

        return randomCenterLoc;
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent e) {
        if (gameInProgress) {
            if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {

                Player gotHit = (Player) e.getEntity();
                Player whoHit = (Player) e.getDamager();

                if(whoHit.getUniqueId()==hunter.getUniqueId() && gotHit.getUniqueId()==runner.getUniqueId()){
                    stopTheGame(whoHit);
                }
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (gameInProgress) {
            Player theOneWhoDied = e.getEntity();
            if(theOneWhoDied.getUniqueId()==hunter.getUniqueId()){
                stopTheGame(runner);
            }
            if(theOneWhoDied.getUniqueId()==runner.getUniqueId()){
                stopTheGame(hunter);
            }
        }
    }

    @EventHandler
    public void onMoveSetCompass(PlayerMoveEvent e) {
        Player movingPlayer = e.getPlayer();

        if(gameLoading) {
            e.setCancelled(true);
        }
        if(gameInProgress){
            if(movingPlayer.getUniqueId()==hunter.getUniqueId()) {
                movingPlayer.setCompassTarget(runner.getLocation());
            }
        }
    }
}

