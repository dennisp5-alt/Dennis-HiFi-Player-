package com.dennis.hifiplayer;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class PlaybackService extends MediaSessionService {
    public static final String FX_REFRESH="com.dennis.hifiplayer.REFRESH_FX";
    private MediaSession mediaSession;
    private ExoPlayer player;
    private SoundEngine engine;
    private final BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent intent) {
            if(engine!=null){
                String id=intent.getStringExtra("media_id");
                if(id!=null && intent.hasExtra("genre"))engine.overrideCurrentGenre(id,intent.getStringExtra("genre"));
                else engine.apply();
            }
        }
    };
    @Override public void onCreate() {
        super.onCreate();
        engine=new SoundEngine(this);
        player=new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA).build(),true);
        player.setHandleAudioBecomingNoisy(true);
        player.setWakeMode(C.WAKE_MODE_LOCAL);
        player.addListener(new Player.Listener() {
            @Override public void onAudioSessionIdChanged(int id) { engine.attach(id); }
            @Override public void onMediaItemTransition(androidx.media3.common.MediaItem item,int reason) {
                engine.updateTrack(item);
            }
            @Override public void onMediaMetadataChanged(androidx.media3.common.MediaMetadata metadata) {
                // Use richer tags decoded by the player if the scanner could not read them.
                engine.updateDecodedMetadata(metadata);
            }
        });
        mediaSession=new MediaSession.Builder(this,player).build();
        IntentFilter filter=new IntentFilter(FX_REFRESH);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(receiver,filter,RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver,filter);
    }
    @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) { return mediaSession; }
    @Override public void onDestroy() {
        try{unregisterReceiver(receiver);}catch(Exception ignored){}
        if(mediaSession!=null) { mediaSession.release(); mediaSession=null; }
        if(engine!=null){engine.release();engine=null;}
        if(player!=null){player.release();player=null;}
        super.onDestroy();
    }
}
