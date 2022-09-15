package cn.daoge.parkour.config;

import cn.nukkit.math.Vector3;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LevelVector3 extends Vector3 {
    protected String levelName;

    public LevelVector3(double x, double y, double z, String levelName) {
        super(x, y, z);
        this.levelName = levelName;
    }
}
