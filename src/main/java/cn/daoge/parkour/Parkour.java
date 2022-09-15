package cn.daoge.parkour;

import cn.daoge.parkour.command.ParkourCommand;
import cn.daoge.parkour.config.ParkourData;
import cn.daoge.parkour.instance.IParkourInstance;
import cn.daoge.parkour.instance.ParkourInstance;
import cn.daoge.parkour.storage.JSONParkourStorage;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerRespawnEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public class Parkour extends PluginBase implements Listener {

    //todo: config control
    public static final int BACK_ITEM_ID = ItemID.COMPASS;
    public static final int INFO_ITEM_ID = ItemID.ENCHANTED_BOOK;
    public static final int PAUSE_ITEM_ID = ItemID.CLOCK;
    public static final int ESCAPE_ITEM_ID = ItemID.BED;

    @Getter
    protected static Parkour instance;
    protected Map<String, IParkourInstance> parkourInstanceMap = new HashMap<>();
    protected Map<Player, IParkourInstance> currentPlayingParkour = new HashMap<>();
    protected Path dataPath;

    {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.dataPath = this.getDataFolder().toPath().resolve("instances");
        if (!Files.exists(this.dataPath)) {
            try {
                Files.createDirectories(this.dataPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        loadParkourInstance();
        Server.getInstance().getCommandMap().register("", new ParkourCommand("parkour"));
        Server.getInstance().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.parkourInstanceMap.forEach((k, v) -> {
            v.save();
        });
    }

    public void joinTo(Player player, IParkourInstance instance) {
        if (!instance.isComplete()) {
            player.sendMessage("[§bParkour§r] §cUncompleted parkour instance, please complete it before playing!");
        }
        this.currentPlayingParkour.put(player, instance);
        instance.join(player);
    }

    public void tpTo(Player player, IParkourInstance instance) {
        instance.tp(player);
    }

    public void quitFromParkour(Player player) {
        if (!currentPlayingParkour.containsKey(player)) return;
        var instance = this.currentPlayingParkour.remove(player);
        instance.quit(player);
    }

    public void sendParkourInfo(Player player, IParkourInstance instance) {
        var data = instance.getData();
        StringBuilder builder = new StringBuilder();
        builder.append("§l§fRanking: \n");
        data.ranking.forEach((name, score) -> {
            builder.append("§l[").append(name).append("]: §b").append(score).append("§f\n");
        });
        FormWindowSimple form = new FormWindowSimple("§l§b Info | §f§l" + data.name, builder.toString());
        player.showFormWindow(form);
    }

    public void sendParkourListForm(Player player) {
        var buttons = this.parkourInstanceMap.values()
                .stream()
                .map(inst -> generateListButton(inst))
                .toList();
        var form = new FormWindowSimple("§f§lParkour", "", buttons);
        form.addHandler((player1, i) -> {
            if (form.getResponse() == null) return;
            var clickedButton = (ParkourElementButton)form.getResponse().getClickedButton();
            tpTo(player1, clickedButton.instance);
        });
        player.showFormWindow(form);
    }

    public void addParkourInstance(IParkourInstance instance) {
        this.parkourInstanceMap.put(instance.getData().name, instance);
    }

    protected ElementButton generateListButton(IParkourInstance instance) {
        var nameBuilder = new StringBuilder();
        nameBuilder.append("§f§l").append(instance.getData().name).append("\n§bPlaying: ").append(instance.getPlayers().size());
        return new ParkourElementButton(nameBuilder.toString(), new ElementButtonImageData("path", "textures/blocks/grass_side_carried.png"), instance);
    }

    protected class ParkourElementButton extends ElementButton {

        @Getter
        @Setter
        protected transient IParkourInstance instance;

        public ParkourElementButton(String text) {
            super(text);
        }

        public ParkourElementButton(String text, ElementButtonImageData image) {
            super(text, image);
        }

        public ParkourElementButton(String text, ElementButtonImageData image, IParkourInstance instance) {
            super(text, image);
            this.instance = instance;
        }
    }

    protected void loadParkourInstance() {
        try (Stream<Path> walk = Files.walk(this.dataPath)) {
            for (var instancePath : walk.filter(Files::isRegularFile).toList()) {
                var instance = createParkourInstance(instancePath);
                addParkourInstance(instance);
                this.getLogger().info("[§bParkour§r] Successfully load parkour instance §a" + instance.getData().name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected IParkourInstance createParkourInstance(Path instancePath) {
        return new ParkourInstance(new JSONParkourStorage(instancePath));
    }

    @EventHandler
    protected void onPlayerMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        var from = event.getFrom().floor();
        var to = event.getTo().floor();
        if (!from.level.getName().equals(to.level.getName()) && currentPlayingParkour.containsKey(player)) {
            quitFromParkour(player);
            return;
        }
        if (!from.equals(to)) {
            if (!currentPlayingParkour.containsKey(player)) {
                parkourInstanceMap.forEach((name, instance) -> {
                    if (instance.isComplete() &&
                            !currentPlayingParkour.containsKey(player) &&
                            player.level.getName().equals(instance.getLevel().getName()) &&
                            instance.getData().start.floor().equals(to)) {
                        joinTo(player, instance);
                    }
                });
            } else {
                var currentPlaying = currentPlayingParkour.get(player);
                if (currentPlaying.isPaused(player)) {
                    return;
                }
                if (currentPlaying.getData().end.floor().equals(to)) {
                    currentPlayingParkour.remove(player);
                    currentPlaying.onReachEnd(player);
                    return;
                }
                for (var routePoint : currentPlaying.getData().routePoints) {
                    if (routePoint.floor().equals(to) && !currentPlaying.getLastPoint(player).floor().equals(to)) {
                        currentPlaying.onReachPoint(player, routePoint.floor().add(0.5, 0, 0.5));
                    }
                }
            }
        }
    }

    @EventHandler
    protected void onPlayerInteractItem(PlayerInteractEvent event) {
        if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_AIR
                && event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        var player = event.getPlayer();
        var currentPlaying = currentPlayingParkour.get(player);
        if (currentPlaying == null) return;
        switch (player.getInventory().getItemInHand().getId()) {
            case BACK_ITEM_ID -> {
                var lastRoutePoint = currentPlaying.getLastPoint(player);
                player.teleport(lastRoutePoint);
            }
            case INFO_ITEM_ID -> {
                sendParkourInfo(player, currentPlaying);
            }
            case PAUSE_ITEM_ID -> {
                currentPlaying.pause(player, !currentPlaying.isPaused(player));
            }
            case ESCAPE_ITEM_ID -> {
                quitFromParkour(player);
            }
        }
    }

    @EventHandler
    protected void onPlayerQuit(PlayerQuitEvent event) {
        quitFromParkour(event.getPlayer());
    }

    @EventHandler
    protected void onPlayerRespawn(PlayerRespawnEvent event) {
        var player = event.getPlayer();
        var currentPlaying = currentPlayingParkour.get(player);
        if (currentPlaying == null) return;
        var lastRoutePoint = currentPlaying.getLastPoint(player);
        event.setRespawnPosition(Position.fromObject(lastRoutePoint, player.level));
    }
}
