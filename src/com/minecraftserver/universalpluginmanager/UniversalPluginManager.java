package com.minecraftserver.universalpluginmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class UniversalPluginManager extends JavaPlugin {

    Logger                    log = Logger.getLogger("MineCraft");
    private boolean           changed, loaded, locked;
    private String            password;
    private UUID              playerID;
    private Player            player;
    private ChatManager       cm;
    private UPM_IOManager     man;
    private YamlConfiguration parentconfig;
    private FileConfiguration pluginconfig;
    private String            loadedplugin;
    private Plugin            pl;
    private String[]          tempargs;
    private String            temptype;
    private List<String>      templist;

    public String getAuthor() {
        List<String> CBAuthor = this.getDescription().getAuthors();
        String CBAuthor_String = CBAuthor.toString();
        return CBAuthor_String;
    }

    public String getVersion() {
        return this.getDescription().getVersion();
    }

    @Override
    public boolean onCommand(CommandSender cmdsender, Command cmd,
            String label, String[] args) {
        if (cmdsender instanceof Player) {
            Player player = (Player) cmdsender;
            if (!cmd.getName().equalsIgnoreCase("upm")) {
                return false;
            } else if (args.length == 0) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: "
                        + ChatColor.GOLD + "/UPM <password> ");
            } else if (!locked) {
                if (args[0].equals(parentconfig.getString("password"))
                        && (player.hasPermission("upm.limited")
                                || player.isOp() || player
                                    .hasPermission("upm.admin"))) {
                    this.player = player;
                    this.playerID = player.getUniqueId();
                    cm.disableChat(player);
                    locked = true;
                    cm.sendMessage(ChatColor.LIGHT_PURPLE + "UPM Mode enabled");
                } else {
                    player.sendMessage(ChatColor.RED
                            + "Wrong password or no permission for this command!");
                }
            } else if (player.getUniqueId() == playerID) {
                if (args[0].equalsIgnoreCase("exit")) {
                    locked = false;
                    changed = false;
                    loaded = false;
                    cm.sendMessage(ChatColor.LIGHT_PURPLE + "UPM Mode disabled");
                    cm.enableChat();
                } else if (args[0].equalsIgnoreCase("list")
                        && (player.hasPermission("upm.limited")
                                || player.isOp() || player
                                    .hasPermission("upm.admin"))) {
                    cm.sendMessage(ChatColor.LIGHT_PURPLE
                            + "Supported Plugins:\n------------");
                    for (String s : getSupportedPlugins())
                        cm.sendMessage(ChatColor.LIGHT_PURPLE + s);
                } else if (args[0].equalsIgnoreCase("load-cfg")
                        && (player.hasPermission("upm.config.show")
                                || player.isOp() || player
                                    .hasPermission("upm.admin"))) {
                    if (args.length > 1 && args[1] != null) {
                        if (getSupportedPlugins().contains(args[1])) {
                            pl = Bukkit.getPluginManager().getPlugin(args[1]);
                            if (changed) {
                                cm.sendMessage(ChatColor.RED
                                        + "There are unsafed changes in the loaded config."
                                        + " You really want to load another config?\n"
                                        + "To confirm type: " + ChatColor.GOLD
                                        + "YES");
                                cm.requestConfirmation(1);
                            } else loadCFG();
                        } else cm.sendMessage(ChatColor.LIGHT_PURPLE
                                + "Plugin " + ChatColor.GOLD + args[1]
                                + ChatColor.LIGHT_PURPLE
                                + " does not exist or isnt supported!");

                    } else cm.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: "
                            + ChatColor.GOLD + "/upm load-cfg <pluginname>");

                } else if (args[0].equalsIgnoreCase("save-cfg")
                        && (player.hasPermission("upm.config.edit")
                                || player.isOp() || player
                                    .hasPermission("upm.admin"))) {
                    if (loaded)
                        if (changed)
                            if (getSupportedPlugins().contains(loadedplugin)) {
                                pl = Bukkit.getPluginManager().getPlugin(
                                        loadedplugin);
                                try {
                                    saveExternalPluginConfig(pl);
                                    cm.sendMessage(ChatColor.LIGHT_PURPLE
                                            + "Config saved successfull");
                                    this.changed = false;
                                    this.loaded = false;
                                } catch (IOException e) {
                                    cm.sendMessage(ChatColor.RED
                                            + "Error while saving config!");
                                }
                            } else cm.sendMessage(ChatColor.LIGHT_PURPLE
                                    + "Plugin " + ChatColor.GOLD + loadedplugin
                                    + ChatColor.LIGHT_PURPLE
                                    + " does not exist or isnt supported!");
                        else cm.sendMessage(ChatColor.LIGHT_PURPLE
                                + "Nothing to save, config isnt changed!");
                    else cm.sendMessage(ChatColor.LIGHT_PURPLE
                            + "Nothing to save, config isnt loaded!");
                } else if (args[0].equalsIgnoreCase("update")
                        && (player.hasPermission("upm.update") || player.isOp() || player
                                .hasPermission("upm.admin"))) {
                    if (args.length > 1 && args[1] != null) {
                        if (getSupportedPlugins().contains(args[1])) {
                            pl = Bukkit.getPluginManager().getPlugin(args[1]);
                            try {
                                String updateURL = pl.getConfig().getString(
                                        "upm_update");
                                updatePlugin(updateURL, pl);
                                cm.sendMessage(ChatColor.LIGHT_PURPLE
                                        + "Plugin updated successfull. Restart needed.");
                            } catch (Exception e) {
                                cm.sendMessage(ChatColor.RED
                                        + "Error while updating!");
                            }
                        } else cm.sendMessage(ChatColor.LIGHT_PURPLE
                                + "Plugin " + ChatColor.GOLD + args[1]
                                + ChatColor.LIGHT_PURPLE
                                + " does not exist or isnt supported!");

                    } else cm.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: "
                            + ChatColor.GOLD + "/upm update <pluginname>");
                } else if (args[0].equalsIgnoreCase("check-updates")
                        && (player.hasPermission("upm.limited")
                                || player.isOp() || player
                                    .hasPermission("upm.admin"))) {
                    cm.sendMessage(ChatColor.LIGHT_PURPLE
                            + "Name || Installed Version || Newest Version || Latest Changes");
                    for (String s : getSupportedPlugins()) {
                        pl = Bukkit.getPluginManager().getPlugin(s);
                        String name = pl.getName();
                        String installedVersion = pl.getDescription()
                                .getVersion();
                        String updateURL = pl.getConfig().getString(
                                "upm_update");
                        String[] information = getLatestPluginInformation(updateURL);
                        if (information != null
                                && !(installedVersion.equals(information[0])))
                            cm.sendMessage(ChatColor.LIGHT_PURPLE + name
                                    + " || " + installedVersion + " || "
                                    + information[0] + " || " + ChatColor.GREEN
                                    + information[1]);
                    }
                } else if (args[0].equalsIgnoreCase("set-cfg")
                        && (player.hasPermission("upm.config.edit")
                                || player.isOp() || player
                                    .hasPermission("upm.admin"))) {
                    if (args.length > 2 && args[1] != null && args[2] != null)
                        if (loaded) {
                            try {
                                String type = pluginconfig
                                        .getString("upm_configtype." + args[1]);
                                if (pluginconfig.get("upm_configtype."
                                        + args[1]) != null) {
                                    if (type.equals("list")) {
                                        List<String> list = (List<String>) pluginconfig
                                                .getList(args[1]);
                                        if (list == null)
                                            list = new Vector<String>();
                                        if (args[2].equalsIgnoreCase("add")
                                                && args.length > 3) {
                                            cm.sendMessage(ChatColor.LIGHT_PURPLE
                                                    + "Adding "
                                                    + args[3]
                                                    + " to list");
                                            cm.sendMessage(ChatColor.RED
                                                    + "To Confirm type "
                                                    + ChatColor.GOLD + "YES");
                                            templist = list;
                                            tempargs = args;
                                            temptype = type;
                                            cm.requestConfirmation(2);
                                        } else if (args[2]
                                                .equalsIgnoreCase("remove")
                                                && args.length > 3) {
                                            cm.sendMessage(ChatColor.LIGHT_PURPLE
                                                    + "Removing "
                                                    + args[3]
                                                    + " from list");
                                            cm.sendMessage(ChatColor.RED
                                                    + "To Confirm type "
                                                    + ChatColor.GOLD + "YES");
                                            templist = list;
                                            tempargs = args;
                                            temptype = type;
                                            cm.requestConfirmation(2);

                                        } else if (args[2]
                                                .equalsIgnoreCase("show")) {
                                            cm.sendMessage(ChatColor.LIGHT_PURPLE
                                                    + "Current List values:");
                                            for (String s : list)
                                                cm.sendMessage("- " + s);
                                        } else cm
                                                .sendMessage(ChatColor.LIGHT_PURPLE
                                                        + "Usage:"
                                                        + ChatColor.GOLD
                                                        + "/UPM set-cfg <path> <add/remove/show> (value)");
                                    } else {
                                        cm.sendMessage(ChatColor.LIGHT_PURPLE
                                                + "Current Value of "
                                                + args[1]
                                                + ":"
                                                + ChatColor.GOLD
                                                + pluginconfig
                                                        .getString(args[1])
                                                + " (" + type + ")");
                                        cm.sendMessage(ChatColor.LIGHT_PURPLE
                                                + "New Value of " + args[1]
                                                + ":" + ChatColor.GOLD
                                                + args[2]);
                                        cm.sendMessage(ChatColor.RED
                                                + "To Confirm type "
                                                + ChatColor.GOLD + "YES");
                                        tempargs = args;
                                        temptype = type;
                                        cm.requestConfirmation(2);
                                    }
                                } else {
                                    cm.sendMessage(ChatColor.LIGHT_PURPLE
                                            + "Path not found! Use /upm add-path to create it.");
                                }
                            } catch (Exception e) {
                                cm.sendMessage(e.getLocalizedMessage());
                                cm.sendMessage(e.getMessage());
                                e.printStackTrace();
                            }
                        } else cm
                                .sendMessage(ChatColor.LIGHT_PURPLE
                                        + "No Plugin loaded! Use "
                                        + ChatColor.GOLD
                                        + "/UPM load-cfg <pluginname> "
                                        + ChatColor.LIGHT_PURPLE
                                        + " to load a Config.");
                    else cm.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: "
                            + ChatColor.GOLD + "/UPM set-cfg <path> <value> ");
                } else if (args[0].equalsIgnoreCase("add-path")
                        && (player.hasPermission("upm.config.edit")
                                || player.isOp() || player
                                    .hasPermission("upm.admin"))) {
                    if (loaded)
                        if (args.length > 2 && args[1] != null
                                && args[2] != null)
                            if (args[2].equalsIgnoreCase("string")) {
                                tempargs = new String[3];
                                // add upm path
                                tempargs[1] = "upm_configtype." + args[1];
                                tempargs[2] = "string";
                                changeValue();
                            } else if (args[2].equalsIgnoreCase("int")) {
                                tempargs = new String[3];
                                // add upm path
                                temptype = "string";
                                tempargs[1] = "upm_configtype." + args[1];
                                tempargs[2] = "int";
                                changeValue();
                            } else if (args[2].equalsIgnoreCase("double")) {
                                tempargs = new String[3];
                                // add upm path
                                temptype = "string";
                                tempargs[1] = "upm_configtype." + args[1];
                                tempargs[2] = "double";
                                changeValue();
                            } else if (args[2].equalsIgnoreCase("boolean")) {
                                tempargs = new String[3];
                                // add upm path
                                temptype = "string";
                                tempargs[1] = "upm_configtype." + args[1];
                                tempargs[2] = "boolean";
                                changeValue();
                            } else if (args[2].equalsIgnoreCase("list")) {
                                tempargs = new String[4];
                                // add upm path
                                temptype = "string";
                                tempargs[1] = "upm_configtype." + args[1];
                                tempargs[2] = "list";
                                changeValue();
                            } else {
                                cm.sendMessage(ChatColor.RED + "Value type "
                                        + args[2] + " isnt supported!");
                            }
                        else cm.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: "
                                + ChatColor.GOLD
                                + "/UPM add-path <path> <type>");
                    else cm.sendMessage(ChatColor.LIGHT_PURPLE
                            + "No Plugin loaded! Use " + ChatColor.GOLD
                            + "/UPM load-cfg <pluginname> "
                            + ChatColor.LIGHT_PURPLE + " to load a Config.");
                } else if (args[0].equalsIgnoreCase("show-cfg")
                        && (player.hasPermission("upm.config.show")
                                || player.isOp() || player
                                    .hasPermission("upm.admin"))) {
                    if (loaded)
                        if (args.length > 1 && args[1] != null)
                            if (pluginconfig.get(args[1]) != null)
                                cm.sendMessage(ChatColor.LIGHT_PURPLE
                                        + "Current Value of " + args[1] + ":"
                                        + ChatColor.GOLD
                                        + pluginconfig.getString(args[1]));
                            else cm.sendMessage(ChatColor.LIGHT_PURPLE
                                    + "Config is not defined for " + args[1]);
                        else cm.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: "
                                + ChatColor.GOLD + "/UPM show-cfg <path> ");
                    else cm.sendMessage(ChatColor.LIGHT_PURPLE
                            + "No Plugin loaded! Use " + ChatColor.GOLD
                            + "/UPM load-cfg <pluginname> "
                            + ChatColor.LIGHT_PURPLE + " to load a Config.");
                } else if (args[0].equalsIgnoreCase("set-password")
                        && (player.hasPermission("upm.config.edit")
                                || player.isOp() || player
                                    .hasPermission("upm.admin"))) {
                    if (args.length > 2 && args[1] != null && args[2] != null) {
                        if (args[1].equals(parentconfig.getString("password"))) {
                            parentconfig.set("password", args[2]);
                            UPM_IOManager.saveConfig(parentconfig);
                            cm.sendMessage(ChatColor.GREEN
                                    + "Password successfull changed");
                        } else cm.sendMessage(ChatColor.RED
                                + "Wrong current Password");
                    } else cm
                            .sendMessage(ChatColor.LIGHT_PURPLE
                                    + "Usage: "
                                    + ChatColor.GOLD
                                    + "/UPM set-password <current Password> <new Password>");
                } else cm.sendMessage(ChatColor.LIGHT_PURPLE + "Help:\n"
                        + ChatColor.GOLD + "/UPM exit" + ChatColor.GRAY + " - "
                        + ChatColor.LIGHT_PURPLE
                        + "Leaves UPM Mode, enables chat again\n"
                        + ChatColor.GOLD + "/UPM check-updates"
                        + ChatColor.GRAY + " - " + ChatColor.LIGHT_PURPLE
                        + "Shows latest Updates for all supported Plugins\n"
                        + ChatColor.GOLD + "/UPM update" + ChatColor.GRAY
                        + " - " + ChatColor.LIGHT_PURPLE
                        + "Updates a plugin to latest online version\n"
                        + ChatColor.GOLD + "/UPM load-cfg" + ChatColor.GRAY
                        + " - " + ChatColor.LIGHT_PURPLE
                        + "Loads a config of a plugin\n" + ChatColor.GOLD
                        + "/UPM save-cfg" + ChatColor.GRAY + " - "
                        + ChatColor.LIGHT_PURPLE + "Saves the loaded config\n"
                        + ChatColor.GOLD + "/UPM set-cfg" + ChatColor.GRAY
                        + " - " + ChatColor.LIGHT_PURPLE
                        + "Sets a value for a config path\n" + ChatColor.GOLD
                        + "/UPM show-cfg" + ChatColor.GRAY + " - "
                        + ChatColor.LIGHT_PURPLE
                        + "Shows a value of a config path\n" + ChatColor.GOLD
                        + "/UPM set-password" + ChatColor.GRAY + " - "
                        + ChatColor.LIGHT_PURPLE + "Changes UPM password");

            } else {
                player.sendMessage(ChatColor.LIGHT_PURPLE
                        + "UPM is currently used by " + ChatColor.GOLD
                        + this.player.getName());
            }
        }
        return true;
    }

    @Override
    public void onDisable() {
        cm.enableChat();
        UPM_IOManager.saveConfig(parentconfig);
        log.info("[UPM] Disabled system!");
    }

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdir();
        init();

        log.info("[UPM] Enabled system, version: " + getVersion() + "!");
    }

    private String[] getLatestPluginInformation(String u) {
        InputStreamReader ir;
        URL url;
        try {
            url = new URL(u);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.connect();
            int res = con.getResponseCode();
            if (res != 200) {
                return null;
            }
            ir = new InputStreamReader(con.getInputStream());

            String nv;
            String cl;
            try {
                JSONParser jp = new JSONParser();
                Object o = jp.parse(ir);

                if (!(o instanceof JSONObject)) {
                    ir.close();
                    return null;
                }
                JSONObject jo = (JSONObject) o;
                jo = (JSONObject) jo.get("versions");
                nv = (String) jo.get("version");
                cl = (String) jo.get("changelog");
                String[] result = new String[2];
                result[0] = nv;
                result[1] = cl;
                return result;
            } catch (Exception e) {
                ir.close();
                return null;
            }
        } catch (Exception e1) {
            cm.sendMessage("ERROR: Probably an URL error!!");
        }
        return null;
    }

    private String getPluginUpdateURL(String u) {
        InputStreamReader ir;
        URL url;
        try {
            url = new URL(u);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.connect();
            int res = con.getResponseCode();
            if (res != 200) {
                return null;
            }
            ir = new InputStreamReader(con.getInputStream());

            String updateURL;
            try {
                JSONParser jp = new JSONParser();
                Object o = jp.parse(ir);

                if (!(o instanceof JSONObject)) {
                    ir.close();
                    return null;
                }

                JSONObject jo = (JSONObject) o;
                jo = (JSONObject) jo.get("versions");
                updateURL = (String) jo.get("download");
                return updateURL;
            } catch (Exception e) {
                ir.close();
                return null;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private List<String> getSupportedPlugins() {
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        List<String> supportedPlugins = new Vector<String>();
        for (Plugin p : plugins)
            try {
                if (p.getConfig().getBoolean("upm"))
                    supportedPlugins.add(p.getName());
            } catch (Exception e) {
                ;
            }
        return supportedPlugins;
    }

    private void init() {
        cm = new ChatManager(this);
        man = new UPM_IOManager();
        UPM_IOManager.init(this);
        getServer().getPluginManager().registerEvents(cm, this);
        parentconfig = UPM_IOManager.loadConfig();
    }

    private FileConfiguration loadExternalConfig(Plugin pl) {
        File filePath = new File(pl.getDataFolder(), "config.yml");
        YamlConfiguration config = new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(filePath);
    }

    private void saveExternalPluginConfig(Plugin pl) throws IOException {
        File filePath = new File(pl.getDataFolder(), "config.yml");
        pluginconfig.save(filePath);
    }

    private boolean updatePlugin(final String u, final Plugin plugin) {
        final BukkitScheduler bs = plugin.getServer().getScheduler();
        bs.scheduleAsyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                String out;
                try {
                    String updateURL = getPluginUpdateURL(u);
                    if (u == null) return;
                    File to = new File(
                            plugin.getServer().getUpdateFolderFile(), updateURL
                                    .substring(updateURL.lastIndexOf('/') + 1,
                                            updateURL.length()));
                    File tmp = new File(to.getPath() + ".au");
                    if (!tmp.exists()) {
                        plugin.getServer().getUpdateFolderFile().mkdirs();
                        tmp.createNewFile();
                    }
                    URL url = new URL(updateURL);
                    InputStream is = url.openStream();
                    OutputStream os = new FileOutputStream(tmp);
                    byte[] buffer = new byte[4096];
                    int fetched;
                    while ((fetched = is.read(buffer)) != -1)
                        os.write(buffer, 0, fetched);
                    is.close();
                    os.flush();
                    os.close();
                    if (to.exists()) to.delete();
                    tmp.renameTo(to);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return false;
    }

    void changeValue() {
        try {
            int error;
            if (tempargs == null || temptype == null) error = 2 / 0;
            String type = temptype;
            String[] args = tempargs;
            List<String> list = templist;
            // Error fix: try to get real value of args[2]
            if (type.equals("double")) {
                double args2 = Double.parseDouble(args[2]);
                pluginconfig.set(args[1], args2);
            } else if (type.equals("int")) {
                int args2 = Integer.parseInt(args[2]);
                pluginconfig.set(args[1], args2);
            } else if (type.equals("list") && list != null) {
                if (args[2].equalsIgnoreCase("add")) {
                    if (!list.contains(args[3])) {
                        list.add(args[3]);
                        pluginconfig.set(args[1], list);
                    }
                } else if (args[2].equalsIgnoreCase("remove")) {
                    if (list.contains(args[3])) {
                        list.remove(args[3]);
                        pluginconfig.set(args[1], list);
                    }
                } else error = 2 / 0;
            } else if (type.equals("boolean")) {
                if (args[2].equals("true") || args[2].equals("yes")) {
                    boolean args2 = true;
                    pluginconfig.set(args[1], args2);
                } else if (args[2].equals("false") || args[2].equals("no")) {
                    boolean args2 = false;
                    pluginconfig.set(args[1], args2);
                } else error = 2 / 0;
            } else pluginconfig.set(args[1], args[2]); // its
                                                       // a
                                                       // string
            cm.sendMessage(ChatColor.GREEN + "Config changed!");
            changed = true;
            templist = null;
            tempargs = null;
            temptype = null;
        } catch (Exception e) {
            templist = null;
            tempargs = null;
            temptype = null;
            cm.sendMessage(ChatColor.RED + "Invalid value for config node!");
        }
    }

    void loadCFG() {
        pluginconfig = loadExternalConfig(pl);
        cm.sendMessage(ChatColor.LIGHT_PURPLE + "Config loaded successfull");
        this.loaded = true;
        this.changed = false;
        this.loadedplugin = pl.getName();
    }
}
