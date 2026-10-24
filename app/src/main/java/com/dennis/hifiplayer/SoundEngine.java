package com.dennis.hifiplayer;

import android.content.Context;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import androidx.media3.common.MediaMetadata;

/** All effects are attached only to our playback audio session, never to the device-wide mix. */
public final class SoundEngine {
    private Equalizer eq;
    private LoudnessEnhancer loudness;
    private int currentSession=-1;
    private final AppPrefs prefs;
    private MediaMetadata metadata;
    private String status="Original · no processing";

    public SoundEngine(Context c) { prefs=new AppPrefs(c); }
    public String status(){return status;}
    public void updateTrack(MediaMetadata metadata) { this.metadata=metadata; apply(); }
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
        AutoProfile profile=AutoProfile.forTrack(metadata);
        status="auto".equals(mode)?"Auto · "+profile.name:"manual".equals(mode)?"Manual EQ":"Original · no EQ";
        if(eq!=null) {
            try {
                if(!active){eq.setEnabled(false);} else {
                    short bands=eq.getNumberOfBands();
                    short[] range=eq.getBandLevelRange();
                    for(short b=0;b<bands;b++) {
                        float freq=eq.getCenterFreq(b)/1000f;
                        float gain="auto".equals(mode)?interpolate(freq,profile.gains):interpolate(freq,manualGains());
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
                else { loudness.setTargetGain(boost*50); loudness.setEnabled(true); }
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
