package cn.daoge.parkour.storage;

import cn.daoge.parkour.config.ParkourData;

public interface IParkourStorage {
    ParkourData read();
    void save(ParkourData data);
}
