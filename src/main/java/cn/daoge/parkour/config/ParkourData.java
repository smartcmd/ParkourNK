package cn.daoge.parkour.config;

import cn.nukkit.math.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParkourData {
    public String levelName;
    public Vector3 start;
    public List<Vector3> routePoints = new ArrayList<>();
    public Vector3 end;
    public String name;
    //     playerName TimeUsed
    public Map<String, Double> ranking = new HashMap<>();
    public List<Vector3> rankingTextPos = new ArrayList<>();
}
