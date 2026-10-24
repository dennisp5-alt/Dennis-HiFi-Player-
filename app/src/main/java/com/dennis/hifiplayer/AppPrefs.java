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
    public int boost() { return Math.min(12,Math.max(0,prefs.getInt("booster_half_db", 0))); }
    public void boost(int halfDb) { prefs.edit().putInt("booster_half_db",Math.max(0,Math.min(12,halfDb))).apply(); }
    public String preset(){return prefs.getString("eq_preset","country");}
    public void preset(String id){prefs.edit().putString("eq_preset",id).apply();}
    public String trackGenre(String mediaId){return prefs.getString("genre_track_"+mediaId,"");}
    public void trackGenre(String mediaId,String id){
        SharedPreferences.Editor e=prefs.edit();
        if(id==null||id.isEmpty())e.remove("genre_track_"+mediaId);
        else e.putString("genre_track_"+mediaId,id);
        e.apply();
    }
    public String folderGenre(String rootUri){return prefs.getString("genre_folder_"+rootUri,"");}
    public void folderGenre(String rootUri,String id){
        SharedPreferences.Editor e=prefs.edit();
        if(id==null||id.isEmpty())e.remove("genre_folder_"+rootUri);
        else e.putString("genre_folder_"+rootUri,id);
        e.apply();
    }
    public String effectiveGenre(Song song){
        String override=trackGenre(song.uri.toString());if(!override.isEmpty())return override;
        override=folderGenre(song.rootUri);if(!override.isEmpty())return override;
        return song.genre;
    }
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
