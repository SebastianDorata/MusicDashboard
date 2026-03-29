package com.sebastiandorata.musicdashboard.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.FavouriteService;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaylistService;
import javafx.scene.Node;

import java.util.function.BiConsumer;


public class LibraryHandler {

    public final MusicPlayerService musicPlayerService;
    public final PlaylistService    playlistService;
    public final FavouriteService   favouriteService;


    public final BiConsumer<Song, Node> onSongMenu;

    public LibraryHandler(MusicPlayerService musicPlayerService,
                          PlaylistService    playlistService,
                          FavouriteService   favouriteService,
                          BiConsumer<Song, Node> onSongMenu) {
        this.musicPlayerService = musicPlayerService;
        this.playlistService    = playlistService;
        this.favouriteService   = favouriteService;
        this.onSongMenu         = onSongMenu;
    }
}