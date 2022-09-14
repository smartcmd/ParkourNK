package cn.daoge.parkour.command;

import cn.daoge.parkour.Parkour;
import cn.daoge.parkour.instance.ParkourInstance;
import cn.daoge.parkour.storage.JSONParkourStorage;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.command.exceptions.CommandSyntaxException;
import cn.nukkit.command.utils.CommandParser;
import cn.nukkit.lang.TranslationContainer;

public class ParkourCommand extends Command {
    public ParkourCommand(String name) {
        super(name, "Parkour Plugin Main Command");
        this.commandParameters.clear();
        this.commandParameters.put("create", new CommandParameter[]{
                CommandParameter.newEnum("create", new String[]{"create"}),
                CommandParameter.newType("name", CommandParamType.STRING)
        });
        this.commandParameters.put("set start", new CommandParameter[]{
                CommandParameter.newEnum("start", new String[]{"start"}),
                CommandParameter.newType("name", CommandParamType.STRING),
                CommandParameter.newType("pos", true, CommandParamType.POSITION)
        });
        this.commandParameters.put("set end", new CommandParameter[]{
                CommandParameter.newEnum("end", new String[]{"end"}),
                CommandParameter.newType("name", CommandParamType.STRING),
                CommandParameter.newType("pos", true, CommandParamType.POSITION)
        });
        this.commandParameters.put("add route point", new CommandParameter[]{
                CommandParameter.newEnum("addPoint", new String[]{"addPoint"}),
                CommandParameter.newType("name", CommandParamType.STRING),
                CommandParameter.newType("pos", true, CommandParamType.POSITION)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] args) {
        if (!this.testPermission(sender)) {
            return false;
        }

        CommandParser parser = new CommandParser(this, sender, args);
        try {
            String form = parser.matchCommandForm();

            if (form == null) {
                sender.sendMessage(new TranslationContainer("commands.generic.usage", "\n" + this.getCommandFormatTips()));
                return false;
            }

            var plugin = Parkour.getInstance();

            switch (form) {
                case "create" -> {
                    parser.parseString();
                    var name = parser.parseString();
                    var dataPath = plugin.getDataPath().resolve(name);
                    var instance = new ParkourInstance(new JSONParkourStorage(dataPath));
                    instance.getData().name = name;
                    instance.getData().levelName = sender.getPosition().level.getName();
                    plugin.addParkourInstance(instance);
                    instance.save();
                    sender.sendMessage("§l§aSuccessfully add parkour §c" + name);
                }
                case "set start", "set end", "add route point" -> {
                    parser.parseString();
                    var name = parser.parseString();
                    var instance = plugin.getParkourInstanceMap().get(name);
                    var pos = parser.hasNext() ? parser.parseVector3() : sender.getPosition();
                    if (form.equals("set start")) {
                        instance.getData().start = pos.floor().add(0.5, 0, 0.5);
                        instance.save();
                        sender.sendMessage("§l§aSuccessfully set start of parkour §c" + name);
                    } else if (form.equals("set end")) {
                        instance.getData().end = pos.floor().add(0.5, 0, 0.5);
                        instance.save();
                        sender.sendMessage("§l§aSuccessfully set end of parkour §c" + name);
                    } else {
                        instance.getData().routePoints.add(pos.floor().add(0.5, 0, 0.5));
                        instance.save();
                        sender.sendMessage("§l§aSuccessfully add route point to parkour §c" + name);
                    }
                }
            }
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
