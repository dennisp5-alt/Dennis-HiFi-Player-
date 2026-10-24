package com.dennis.hifiplayer;

import androidx.media3.common.MediaMetadata;
import java.util.Locale;

/** Listening presets (dB) at 80 Hz, 250 Hz, 1 kHz, 4 kHz and 12 kHz. */
public final class AutoProfile {
    public final String id, name;
    public final float[] gains;
    private AutoProfile(String id,String name,float...gains){this.id=id;this.name=name;this.gains=gains;}

    public static final String[] IDS={"country","rock","edm","pop","rap","rnb","fifties","christmas","jazz","acoustic","classical"};
    public static final String[] NAMES={"Country","Rock","EDM / Eurodance","Pop","Rap / Hip-hop","R&B / Soul","50s / Oldies","Christmas / Holiday","Jazz / Blues","Acoustic / Folk","Classical / Natural"};

    public static AutoProfile preset(String id){
        if(id==null)return neutral();
        switch(id){
            case "country": return new AutoProfile(id,"Country",.6f,.1f,.4f,.6f,.2f);
            case "rock": return new AutoProfile(id,"Rock",.8f,-.5f,.2f,.9f,.5f);
            case "edm": return new AutoProfile(id,"EDM / Eurodance",1.6f,-.5f,-.5f,.5f,.8f);
            case "pop": return new AutoProfile(id,"Pop",.5f,-.2f,.4f,.6f,.4f);
            case "rap": return new AutoProfile(id,"Rap / Hip-hop",1.5f,-.4f,-.3f,.4f,.3f);
            case "rnb": return new AutoProfile(id,"R&B / Soul",1f,.3f,.4f,.2f,.4f);
            case "fifties": return new AutoProfile(id,"50s / Oldies",.2f,.3f,.5f,.1f,-.5f);
            case "christmas": return new AutoProfile(id,"Christmas / Holiday",.3f,.2f,.4f,.3f,.2f);
            case "jazz": return new AutoProfile(id,"Jazz / Blues",.2f,.4f,.2f,.2f,.1f);
            case "acoustic": return new AutoProfile(id,"Acoustic / Folk",.2f,.2f,.5f,.3f,.1f);
            case "classical": return new AutoProfile(id,"Classical / Natural",0f,0f,0f,0f,0f);
            default: return neutral();
        }
    }
    public static AutoProfile forTrack(MediaMetadata m){return classify(m==null?"":m.genre==null?"":m.genre.toString());}
    public static AutoProfile classify(String genre){return preset(classifyId(genre));}
    public static String classifyId(String genre){
        if(genre==null || genre.trim().isEmpty())return "neutral";
        String g=genre.toLowerCase(Locale.ROOT).trim();
        // Some ID3v1/TCON readers expose numeric genres instead of their name.
        String number=g.replace("(","").replace(")","").trim();
        if(number.matches("[0-9]{1,3}")){
            switch(Integer.parseInt(number)){
                case 0:case 8:case 29:case 30:case 74:case 85:case 96:return "jazz";
                case 2:case 89:return "country";
                case 13:case 28:case 46:case 53:case 60:case 62:case 98:return "pop";
                case 5:case 14:case 42:return "rnb";
                case 7:case 15:case 27:case 59:case 61:return "rap";
                case 1:case 6:case 9:case 17:case 20:case 22:case 40:case 43:case 47:case 56:case 79:case 81:case 91:case 92:case 93:case 94:case 95:return "rock";
                case 3:case 4:case 18:case 19:case 25:case 31:case 35:case 51:case 52:case 54:case 63:case 68:return "edm";
                case 24:case 26:case 32:case 33:case 44:case 45:return "classical";
                case 11:case 76:case 78:case 83:return "fifties";
                case 80:case 82:case 88:return "acoustic";
                default:return "neutral";
            }
        }
        if(has(g,"christmas","xmas","holiday","seasonal","carol","festive"))return "christmas";
        if(has(g,"1950","1950s","50s","oldies","doo wop","doo-wop","rockabilly","rock and roll","rock 'n' roll","rock & roll","swing revival"))return "fifties";
        if(has(g,"eurodance","euro dance","eurobeat","dance","electro","edm","house","techno","trance","disco","dubstep","hardstyle","synthwave","synth-pop","synthpop","garage","breakbeat"))return "edm";
        if(has(g,"r&b","rnb","r 'n' b","rhythm and blues","neo soul","neosoul","soul","motown","funk"))return "rnb";
        if(has(g,"hip hop","hip-hop","hiphop","rap","trap","drill"))return "rap";
        if(has(g,"country","bluegrass","honky tonk","honky-tonk","americana","bush ballad"))return "country";
        if(has(g,"rock","metal","punk","grunge","alternative rock","hard rock"))return "rock";
        if(has(g,"folk","acoustic","singer songwriter","singer-songwriter","unplugged"))return "acoustic";
        if(has(g,"jazz","blues","bebop","bossa nova"))return "jazz";
        if(has(g,"classical","orchestral","opera","symphony","soundtrack","ambient","instrumental"))return "classical";
        if(has(g,"pop","vocal","indie pop","adult contemporary","easy listening"))return "pop";
        return "neutral";
    }
    public static AutoProfile neutral(){return new AutoProfile("neutral","Uncategorised / neutral",0f,0f,0f,0f,0f);}
    private static boolean has(String genre,String...keys){for(String k:keys)if(genre.contains(k))return true;return false;}
}
