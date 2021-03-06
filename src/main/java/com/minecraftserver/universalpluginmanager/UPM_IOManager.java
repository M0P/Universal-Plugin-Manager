package com.minecraftserver.universalpluginmanager;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UPM_IOManager {
    static private UniversalPluginManager plugin;
    static private YamlConfiguration      config = new YamlConfiguration();
    static private File                   configFile;

    public static void init(UniversalPluginManager parent) {
        plugin = parent;
        configFile = new File(plugin.getDataFolder(), "config.yml");

    }

    private static void firstRun() {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            copy(plugin.getResource("config.yml"), configFile);
        }
    }

    private static void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static YamlConfiguration loadConfig() {
        try {
            firstRun();
            config.load(configFile);
            return config;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // not needed yet, future use
    public static void saveConfig(YamlConfiguration newconfig) {
        try {
            newconfig.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
