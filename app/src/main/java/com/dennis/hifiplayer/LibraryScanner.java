package com.dennis.hifiplayer;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;
import java.util.*;
import java.util.concurrent.*;

public final class LibraryScanner {
    private static final Set<String> SUPPORTED = new HashSet<>(Arrays.asList(
            "mp3","flac","wav","m4a","mp4","aac","ogg","opus","3gp","amr"));
    private static final int MAX_TRACKS = 12000;
    private static final int MAX_DEPTH = 24;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile int generation = 0;

    public interface Listener {
        void onComplete(List<Song> songs, List<String> unavailable, boolean truncated);
    }

    public LibraryScanner(Context context) { this.context = context.getApplicationContext(); }
    public void scan(List<String> folders, Listener listener) {
        final int current=++generation;
        executor.submit(()->{
            List<Song> songs=new ArrayList<>();
            List<String> unavailable=new ArrayList<>();
            Set<String> seen=new HashSet<>();
            for (String folder:folders) {
                if (current!=generation || songs.size()>=MAX_TRACKS) break;
                try {
                    Uri treeUri=Uri.parse(folder);
                    DocumentFile root=DocumentFile.fromTreeUri(context,treeUri);
                    if(root==null || !root.canRead() || !root.isDirectory()) { unavailable.add(folder); continue; }
                    walk(root, root.getName()==null?"Selected music":root.getName(),folder,0,songs,seen,current);
                } catch (Exception e) { unavailable.add(folder); }
            }
            songs.sort((a,b)->{
                int c=a.artist.compareToIgnoreCase(b.artist);
                return c!=0?c:a.title.compareToIgnoreCase(b.title);
            });
            if(current==generation) listener.onComplete(songs,unavailable,songs.size()>=MAX_TRACKS);
        });
    }
    private void walk(DocumentFile directory, String name, String rootUri, int depth, List<Song> out,
                      Set<String> seen, int requested) {
        if(depth>MAX_DEPTH || out.size()>=MAX_TRACKS || requested!=generation) return;
        DocumentFile[] children;
        try { children=directory.listFiles(); } catch(Exception e) { return; }
        for (DocumentFile child:children) {
            if(requested!=generation || out.size()>=MAX_TRACKS) return;
            try {
                if (child.isDirectory()) { walk(child,name+" / "+child.getName(),rootUri,depth+1,out,seen,requested); continue; }
                if(!child.isFile()) continue;
                String filename=child.getName();
                if(filename==null) continue;
                int dot=filename.lastIndexOf('.');
                if(dot<0) continue;
                String ext=filename.substring(dot+1).toLowerCase(Locale.ROOT);
                if(!SUPPORTED.contains(ext)) continue;
                String id=child.getUri().toString();
                if(!seen.add(id)) continue;
                Song song=readMetadata(child.getUri(),filename.substring(0,dot),ext,name,rootUri);
                out.add(song);
            } catch(Exception ignored) { }
        }
    }
    private Song readMetadata(Uri uri,String filename,String ext,String folder,String rootUri) {
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        String title=filename,artist="Unknown artist",album="",genre="";
        long duration=0;
        try {
            r.setDataSource(context,uri);
            String v=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if(v!=null && !v.isBlank())title=v;
            v=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if(v!=null && !v.isBlank())artist=v;
            v=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if(v!=null)album=v;
            v=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            if(v!=null)genre=v;
            v=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if(v!=null)duration=Long.parseLong(v);
        } catch(Exception ignored) { }
        finally { try {r.release();}catch(Exception ignored){} }
        return new Song(uri,title,artist,album,genre,folder,rootUri,ext.toUpperCase(Locale.ROOT),duration);
    }
    public void close() { ++generation; executor.shutdownNow(); }
}
