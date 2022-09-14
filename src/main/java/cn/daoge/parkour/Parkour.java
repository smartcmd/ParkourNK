package cn.daoge.parkour;

import cn.daoge.parkour.command.ParkourCommand;
import cn.daoge.parkour.instance.IParkourInstance;
import cn.daoge.parkour.instance.ParkourInstance;
import cn.daoge.parkour.storage.IParkourStorage;
import cn.daoge.parkour.storage.JSONParkourStorage;
import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public class Parkour extends PluginBase {
    {
        instance = this;
    }
    @Getter
    protected static Parkour instance;

    protected Map<String, IParkourInstance> parkourInstanceMap = new HashMap<>();
    protected Path dataPath;

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
    }

    @Override
    public void onDisable() {
        this.parkourInstanceMap.forEach((k, v) -> {
            v.save();
        });
    }

    public void addParkourInstance(IParkourInstance instance) {
        this.parkourInstanceMap.put(instance.getData().name, instance);
    }

    protected void loadParkourInstance() {
        try (Stream<Path> walk = Files.walk(this.dataPath)) {
            for (var instancePath : walk.filter(Files::isRegularFile).toList()) {
                var instance = createParkourInstance(instancePath);
                addParkourInstance(instance);
                this.getLogger().info("§l§aSuccessfully load parkour instance §e" + instance.getData().name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected IParkourInstance createParkourInstance(Path instancePath) {
        return new ParkourInstance(new JSONParkourStorage(instancePath));
    }
}
