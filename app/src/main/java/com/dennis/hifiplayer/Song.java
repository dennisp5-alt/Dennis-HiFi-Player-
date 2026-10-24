package com.dennis.hifiplayer;

import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

public final class Song {
    public final Uri uri;
    public final String title, artist, album, genre, folder, rootUri, extension;
    public final long durationMs;

    public Song(Uri uri, String title, String artist, String album, String genre,
                String folder, String rootUri, String extension, long durationMs) {
        this.uri = uri;
        this.title = blank(title) ? "Unknown track" : title;
        this.artist = blank(artist) ? "Unknown artist" : artist;
        this.album = blank(album) ? "" : album;
        this.genre = blank(genre) ? "" : genre;
        this.folder = folder;
        this.rootUri = rootUri;
        this.extension = extension;
        this.durationMs = Math.max(0, durationMs);
    }
    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }
    public MediaItem asMediaItem() {
        return new MediaItem.Builder().setUri(uri).setMediaId(uri.toString())
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(title).setArtist(artist).setAlbumTitle(album)
                        .setGenre(genre).build())
                .build();
    }
}
