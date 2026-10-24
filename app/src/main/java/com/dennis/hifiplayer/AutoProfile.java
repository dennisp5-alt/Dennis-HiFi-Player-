package com.dennis.hifiplayer;

import androidx.media3.common.MediaMetadata;
import java.util.Locale;

/** Conservative genre-tag-aware EQ. Unknown metadata stays flat; no false AI claim. */
public final class AutoProfile {
    public final String name;
    public final float[] gains;
    private AutoProfile(String name,float...gains) { this.name=name; this.gains=gains; }
    public static AutoProfile forTrack(MediaMetadata m) {
        if(m==null)return neutral();
        return classify(m.genre==null?"":m.genre.toString());
    }
    public static AutoProfile classify(String genre) {
        String g=genre==null?"":genre.toLowerCase(Locale.ROOT);
        if (has(g,"hip hop","hip-hop","rap","trap","r&b","rnb"))
            return new AutoProfile("Hip-hop / R&B",1.4f,.4f,-.5f,.2f,.5f);
        if (has(g,"dance","electro","edm","house","techno","trance","disco","dubstep","hardstyle","synth"))
            return new AutoProfile("Electronic / Dance",1.5f,.2f,-.5f,.6f,.7f);
        if (has(g,"rock","metal","punk","grunge"))
            return new AutoProfile("Rock",.6f,-.5f,.2f,.8f,.4f);
        if (has(g,"acoustic","folk","country","bluegrass","americana"))
            return new AutoProfile("Acoustic / Country",.2f,.2f,.6f,.4f,.1f);
        if (has(g,"jazz","blues","soul"))
            return new AutoProfile("Jazz / Blues",.1f,.5f,.2f,.3f,.1f);
        if (has(g,"classical","orchestral","opera","ambient","soundtrack","instrumental"))
            return new AutoProfile("Natural / Classical",0,0,0,0,0);
        if (has(g,"pop","vocal","indie","alternative"))
            return new AutoProfile("Pop / Vocal",.4f,-.2f,.3f,.6f,.4f);
        return neutral();
    }
    public static AutoProfile neutral(){return new AutoProfile("Neutral (no recognised genre)",0,0,0,0,0);}
    private static boolean has(String g,String...keys) { for(String k:keys)if(g.contains(k))return true;return false; }
}
