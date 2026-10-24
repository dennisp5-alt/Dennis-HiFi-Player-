package com.dennis.hifiplayer;

import android.content.Context;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MediaItem;

/** All effects are attached only to our playback audio session, never to the device-wide mix. */
public final class SoundEngine {
    private Equalizer eq;
    private LoudnessEnhancer loudness;
    private int currentSession=-1;
    private final AppPrefs prefs;
    private MediaMetadata metadata;
    private String mediaId="", currentGenreOverride=null;
    private String status="Original · no processing";

    public SoundEngine(Context c) { prefs=new AppPrefs(c); }
    public String status(){return status;}
    public void updateTrack(MediaItem item){
        metadata=item==null?null:item.mediaMetadata;
        mediaId=item==null?"":item.mediaId;
        currentGenreOverride=null;
        apply();
    }
    public void updateDecodedMetadata(MediaMetadata decoded){
        // Media3 may decode richer tags, but a blank decoded field must not
        // overwrite the genre resolved by the scanner or user folder mapping.
        if(decoded!=null && decoded.genre!=null && !decoded.genre.toString().trim().isEmpty()
                && (metadata==null || metadata.genre==null || metadata.genre.toString().isEmpty())){
            metadata=decoded;
        }
        apply();
    }
    public void overrideCurrentGenre(String id,String genre){
        if(!mediaId.isEmpty() && mediaId.equals(id)) currentGenreOverride=genre;
        apply();
    }
    public void attach(int sessionId) {
        if (sessionId<=0 || sessionId==currentSession) return;
        release(); currentSession=sessionId;
        try { eq=new Equalizer(1000, sessionId); } catch(Exception | LinkageError ignored) { eq=null; }
        try { loudness=new LoudnessEnhancer(sessionId); } catch(Exception | LinkageError ignored){ loudness=null; }
        apply();
    }
    public void apply() {
        String mode=prefs.mode();
        boolean active=!"off".equals(mode);
        String currentGenre=currentGenreOverride!=null?currentGenreOverride:
                metadata==null || metadata.genre==null?"":metadata.genre.toString();
        String trackOverride=prefs.trackGenre(mediaId);
        if(!trackOverride.isEmpty())currentGenre=trackOverride;
        AutoProfile profile=AutoProfile.classify(currentGenre);
        AutoProfile fixed=AutoProfile.preset(prefs.preset());
        status="auto".equals(mode)?"Auto · "+profile.name:
                "preset".equals(mode)?"Preset · "+fixed.name:
                "manual".equals(mode)?"Manual EQ":"Original · no EQ";
        if(eq!=null) {
            try {
                if(!active){eq.setEnabled(false);} else {
                    short bands=eq.getNumberOfBands();
                    short[] range=eq.getBandLevelRange();
                    for(short b=0;b<bands;b++) {
                        float freq=eq.getCenterFreq(b)/1000f;
                        float[] curve="auto".equals(mode)?profile.gains:
                                "preset".equals(mode)?fixed.gains:manualGains();
                        float gain=interpolate(freq,curve);
                        // Keep Auto gentle and protect against unexpected vendor band mappings.
                        int millibels=Math.round(gain*100);
                        millibels=Math.min(range[1],Math.max(range[0],millibels));
                        eq.setBandLevel(b,(short)millibels);
                    }
                    eq.setEnabled(true);
                }
            } catch(Exception | LinkageError ignored) { status+=" · EQ unavailable"; }
        } else if(active) status+=" · EQ unavailable";
        if(loudness!=null) {
            try {
                int boost=prefs.boost();
                if(boost==0) loudness.setEnabled(false);
                else { loudness.setTargetGain(boost*50); loudness.setEnabled(true); } // 0.5 dB steps, up to +6 dB
            } catch(Exception | LinkageError ignored) { status+=" · Boost unavailable"; }
        }
    }
    private float[] manualGains() {
        float[] v=new float[5];
        for(int i=0;i<5;i++)v[i]=prefs.gain(i);
        return v;
    }
    private static float interpolate(float hz,float[] gains) {
        float[] points={80f,250f,1000f,4000f,12000f};
        if(hz<=points[0])return gains[0];
        if(hz>=points[4])return gains[4];
        for(int i=0;i<4;i++) {
            if(hz<=points[i+1]) {
                double t=Math.log(hz/points[i])/Math.log(points[i+1]/points[i]);
                return gains[i]+(float)t*(gains[i+1]-gains[i]);
            }
        }
        return 0;
    }
    public void release() {
        if(eq!=null) {try{eq.setEnabled(false);eq.release();}catch(Exception ignored){} eq=null;}
        if(loudness!=null){try{loudness.setEnabled(false);loudness.release();}catch(Exception ignored){} loudness=null;}
        currentSession=-1;
    }
}
