package cn.daoge.parkour.storage;

import cn.daoge.parkour.config.ParkourData;
import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JSONParkourStorage implements IParkourStorage {

    protected static Gson gson = new Gson();
    protected Path file;

    public JSONParkourStorage(Path file) {
        try {
            this.file = file;
            if (!Files.exists(this.file)) {
                Files.createFile(this.file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ParkourData read() {
        try {
            return gson.fromJson(new FileReader(this.file.toFile()), ParkourData.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(ParkourData data) {
        try {
            Files.write(this.file, gson.toJson(data).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
