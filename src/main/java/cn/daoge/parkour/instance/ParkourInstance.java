package cn.daoge.parkour.instance;

import cn.daoge.parkour.Parkour;
import cn.daoge.parkour.config.ParkourData;
import cn.daoge.parkour.storage.IParkourStorage;
import cn.lanink.gamecore.ranking.Ranking;
import cn.lanink.gamecore.ranking.RankingAPI;
import cn.lanink.gamecore.ranking.RankingFormat;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.weather.EntityLightning;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import cn.nukkit.scheduler.PluginTask;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class ParkourInstance implements IParkourInstance{

    protected ParkourData data;
    protected IParkourStorage storage;
    protected Map<Player, PlayingData> players = new HashMap<>();
    protected Set<Ranking> rankings = new HashSet<>();

    public ParkourInstance(IParkourStorage storage) {
        this.storage = storage;
        this.data = storage.read();
        if (this.data == null) {
            this.data = new ParkourData();
        }
        if (!this.data.rankingTextPos.isEmpty()) {
            this.data.rankingTextPos.forEach(vector3 -> createRankingText(Position.fromObject(vector3, getLevel())));
        }
        Server.getInstance().getScheduler().scheduleRepeatingTask(new RefreshTask(), 1);
    }

    @Override
    public Level getLevel() {
        var server = Server.getInstance();
        var levelName = this.data.levelName;
        return Server.getInstance().isLevelLoaded(levelName) ? server.getLevelByName(levelName) : (server.loadLevel(levelName) ? server.getLevelByName(levelName) : null);
    }

    @Override
    public void save() {
        this.storage.save(this.data);
    }

    @Override
    public boolean isComplete() {
        return this.getLevel() != null &&
                this.data.start != null &&
                this.data.end != null;
    }

    @Override
    public void join(Player player) {
        setParkourItems(player);
        this.players.put(player, new PlayingData());
        player.sendMessage("[§bParkour§r] Successfully join parkour §a" + this.data.name);
    }

    @Override
    public void quit(Player player) {
        player.getInventory().clearAll();
        this.players.remove(player);
        player.sendMessage("[§bParkour§r] Successfully quit parkour §a" + this.data.name);
    }

    @Override
    public boolean isPlaying(Player player) {
        return this.players.containsKey(player);
    }

    @Override
    public void pause(Player player, boolean pause) {
        var playingData = this.players.get(player);
        playingData.paused = pause;
        if (pause) {
            playingData.pausedLoc = player.getLocation();
            player.sendMessage("[§bParkour§r] Time paused");
        } else {
            player.teleport(playingData.pausedLoc);
            playingData.pausedLoc = null;
            player.sendMessage("[§bParkour§r] Time enabled");
        }
    }

    @Override
    public boolean isPaused(Player player) {
        return this.players.get(player).paused;
    }

    @Override
    public void onReachPoint(Player player, Vector3 point) {
        this.players.get(player).lastPoint = point;
        player.sendMessage("[§bParkour§r] Point reached!");
        player.level.addSound(point, Sound.RANDOM_LEVELUP);
    }

    @Override
    public void onReachEnd(Player player) {
        player.sendMessage("[§bParkour§r] Finished parkour §b" + this.data.name + "§r in §b" + String.format("%.3f", this.players.get(player).timeUsed) + "s§r ! Congratulations!");
        for (var other : player.level.getPlayers().values()) other.sendMessage("[§bParkour§r] Player §b" + player.getName() + "§r Finished parkour §a" + this.data.name + "§r in §b" + String.format("%.3f", this.players.get(player).timeUsed) + "s§r ! Congratulations!");
        player.getInventory().clearAll();
        var playingData = this.players.get(player);
        if (!this.data.ranking.containsKey(player.getName()) || playingData.timeUsed < this.data.ranking.get(player.getName())) {
            this.data.ranking.put(player.getName(), Double.parseDouble(String.format("%.3f", this.players.get(player).timeUsed)));
            updateAllRankingText();
        }
        spawnLightning(player);
        this.players.remove(player);
    }

    @Override
    public Vector3 getLastPoint(Player player) {
        return this.players.get(player).lastPoint;
    }

    @Override
    public void addRankingText(Position pos) {
        this.data.rankingTextPos.add(new Vector3(pos.x, pos.y, pos.z).floor().add(0.5,0.5,0.5));
        createRankingText(pos);
    }

    protected void spawnLightning(Player player) {
        var entity = new EntityLightning(player.getChunk(), EntityLightning.getDefaultNBT(player.clone()));
        entity.setEffect(false);
        entity.spawnToAll();
    }

    protected void createRankingText(Position pos) {
        var ranking = RankingAPI.createRanking(Parkour.getInstance(), this.data.name, pos);HashMap<Integer, Integer> showLine = new HashMap();
        showLine.put(20, 15);
        var format = new RankingFormat("§l§e--- §b%name% §e---", "§l[%player%]: §b%score%", "§l§e> §r§l[%player%]: §b%score% §e<", "", RankingFormat.SortOrder.DESCENDING, showLine);
        ranking.setRankingFormat(format);
        ranking.setRankingList(this.data.ranking);
        this.rankings.add(ranking);
        save();
    }

    protected void updateAllRankingText() {
        this.rankings.forEach(ranking -> ranking.setRankingList(this.data.ranking));
    }

    protected void setParkourItems(Player player) {
        var inventory = player.getInventory();
        inventory.clearAll();
        var back = Item.get(Parkour.BACK_ITEM_ID);
        var info = Item.get(Parkour.INFO_ITEM_ID);
        var pause = Item.get(Parkour.PAUSE_ITEM_ID);
        var escape = Item.get(Parkour.ESCAPE_ITEM_ID);
        back.setItemLockMode(Item.ItemLockMode.LOCK_IN_SLOT);
        info.setItemLockMode(Item.ItemLockMode.LOCK_IN_SLOT);
        pause.setItemLockMode(Item.ItemLockMode.LOCK_IN_SLOT);
        escape.setItemLockMode(Item.ItemLockMode.LOCK_IN_SLOT);
        back.setCustomName("§r§f§lLAST POINT");
        info.setCustomName("§r§f§lPARKOUR INFO");
        pause.setCustomName("§r§f§lPAUSE");
        escape.setCustomName("§r§f§lESCAPE");
        inventory.setItem(1, back);
        inventory.setItem(3, info);
        inventory.setItem(5, pause);
        inventory.setItem(7, escape);
    }

    //记录游玩信息，例如上一个路径点，是否暂停等
    public class PlayingData{
        public boolean paused = false;
        public Vector3 lastPoint = getData().start;
        public Location pausedLoc;
        public double timeUsed = 0;//second
    }

    public class RefreshTask extends PluginTask<Parkour>{

        int lastTick = Server.getInstance().getTick();
        public RefreshTask() {
            super(Parkour.getInstance());
        }

        @Override
        public void onRun(int i) {
            var currentTick = Server.getInstance().getTick();
            var timePassed = 0.05d * (currentTick - lastTick);
            players.forEach((player, playingData) -> {
                if (!playingData.paused) playingData.timeUsed += timePassed;
                player.sendActionBar((playingData.paused ? "§e----Pausing----\n§r" : "") +
                        "Time Used: §a" + String.format("%.3f", playingData.timeUsed), 0, 1, 0);
            });
            lastTick = currentTick;
        }
    }
}
