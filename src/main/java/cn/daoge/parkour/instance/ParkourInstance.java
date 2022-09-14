package cn.daoge.parkour.instance;

import cn.daoge.parkour.config.ParkourData;
import cn.daoge.parkour.storage.IParkourStorage;
import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.utils.LevelException;
import lombok.Getter;

@Getter
public class ParkourInstance implements IParkourInstance{

    protected ParkourData data;
    protected IParkourStorage storage;

    public ParkourInstance(IParkourStorage storage) {
        this.storage = storage;
        this.data = storage.read();
        if (this.data == null) {
            this.data = new ParkourData();
        }
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
}
