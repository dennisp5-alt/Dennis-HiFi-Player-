package com.dennis.hifiplayer;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public final class AppPrefs {
    private final SharedPreferences prefs;
    public AppPrefs(Context c) { prefs = c.getSharedPreferences("hifi_settings", Context.MODE_PRIVATE); }
    public String mode() { return prefs.getString("eq_mode", "off"); }
    public void mode(String mode) { prefs.edit().putString("eq_mode",mode).apply(); }
    public int gain(int i) { return prefs.getInt("eq_"+i,0); }
    public void gain(int i,int db) { prefs.edit().putInt("eq_"+i,db).apply(); }
    public int boost() { return prefs.getInt("booster_half_db", 0); }
    public void boost(int halfDb) { prefs.edit().putInt("booster_half_db",Math.max(0,Math.min(6,halfDb))).apply(); }
    public List<String> folders() {
        List<String> result = new ArrayList<>();
        try {
            JSONArray j = new JSONArray(prefs.getString("selected_folders","[]"));
            for(int i=0;i<j.length();i++) result.add(j.getString(i));
        } catch(Exception ignored) { }
        return result;
    }
    public void addFolder(String uri) {
        List<String> paths=folders();
        if (!paths.contains(uri)) { paths.add(uri); saveFolders(paths); }
    }
    public void removeFolder(String uri) {
        List<String> paths=folders(); paths.remove(uri); saveFolders(paths);
    }
    private void saveFolders(List<String> folders) {
        prefs.edit().putString("selected_folders",new JSONArray(folders).toString()).apply();
    }
}
