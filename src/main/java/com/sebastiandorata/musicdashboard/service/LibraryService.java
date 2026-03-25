package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.AlbumRepository;
import com.sebastiandorata.musicdashboard.repository.ArtistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryService {

    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private SongService      songService;

    public List<Song>   getAllSongs()    { return songService.getAllSongs(); }
    public List<Album>  getAllAlbums()   { return albumRepository.findAll(); }
    public List<Artist> getAllArtists()  { return artistRepository.findAll(); }

    public List<Album> resolveAlbumsForArtist(Artist artist) {
        List<Album> albums = artist.getAlbums();
        if (albums != null && !albums.isEmpty()) return albums;
        if (artist.getSongs() == null) return List.of();

        return artist.getSongs().stream()
                .map(Song::getAlbum)
                .filter(album -> album != null)
                .distinct()
                .collect(Collectors.toList());
    }
}