package cn.daoge.parkour.instance;

import cn.daoge.parkour.config.ParkourData;
import cn.nukkit.level.Level;

public interface IParkourInstance {
    Level getLevel();
    ParkourData getData();
    void save();
    boolean isComplete();
}
