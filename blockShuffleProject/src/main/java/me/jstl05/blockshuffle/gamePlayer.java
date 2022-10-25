package me.jstl05.blockshuffle;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public class gamePlayer {

    public Player onePlayer;
    public Material assignedMaterial;
    public boolean foundTheBlock;

    public gamePlayer(Player p, Material m) {
        onePlayer = p;
        assignedMaterial = m;
        foundTheBlock = false;
    }

    public void setNewBlock(Material mat){
        this.assignedMaterial=mat;
    }
}
