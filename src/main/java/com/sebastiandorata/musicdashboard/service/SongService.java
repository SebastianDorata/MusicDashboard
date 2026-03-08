package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Genre;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import com.sebastiandorata.musicdashboard.repository.AlbumRepository;
import com.sebastiandorata.musicdashboard.repository.ArtistRepository;
import com.sebastiandorata.musicdashboard.repository.GenreRepository;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SongService {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private GenreRepository genreRepository;

    public Song importSong(File file) throws Exception {
        // Check if song already exists by file path
        Optional<Song> existingSong = songRepository.findByFilePath(file.getAbsolutePath());
        if (existingSong.isPresent()) {
            System.out.println("Song already imported: " + file.getName());
            return existingSong.get();
        }

        // Read metadata using JAudioTagger
        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTag();

        // Create Song entity
        Song song = new Song();
        song.setFilePath(file.getAbsolutePath());
        song.setTitle(getTagValue(tag, FieldKey.TITLE, file.getName()));
        song.setDuration(audioFile.getAudioHeader().getTrackLength());
        song.setFileFormat(getFileExtension(file));
        song.setFileSizeBytes(file.length());
        song.setDateFirstListened(LocalDate.now());
        song.setListenCount(0);

        // Audio technical details
        song.setBitRate((int) audioFile.getAudioHeader().getBitRateAsNumber());
        song.setSampleRate((int) audioFile.getAudioHeader().getSampleRateAsNumber());
        song.setChannels(Integer.parseInt(audioFile.getAudioHeader().getChannels()));
        song.setCodec(audioFile.getAudioHeader().getEncodingType());

        // Track number
        String trackStr = getTagValue(tag, FieldKey.TRACK, "0");
        try {
            song.setTrackNum(Integer.parseInt(trackStr.split("/")[0]));
        } catch (NumberFormatException e) {
            song.setTrackNum(0);
        }

        // Process Album
        String albumTitle = getTagValue(tag, FieldKey.ALBUM, "Unknown Album");
        String yearStr = getTagValue(tag, FieldKey.YEAR, null);

        Album album = albumRepository.findByTitle(albumTitle)
                .orElseGet(() -> {
                    Album newAlbum = new Album();
                    newAlbum.setTitle(albumTitle);
                    if (yearStr != null) {
                        try {
                            newAlbum.setReleaseDate(LocalDate.of(Integer.parseInt(yearStr), 1, 1));
                        } catch (NumberFormatException e) {// Invalid year, skip

                        }
                    }
                    return albumRepository.save(newAlbum);
                });
        song.setAlbum(album);

        // Process Artists
        String artistName = getTagValue(tag, FieldKey.ARTIST, "Unknown Artist");
        List<Artist> artists = new ArrayList<>();
        // Fixed: changed regex from ",|;|&" to "[,;&]"
        for (String name : artistName.split("[,;&]")) {
            String trimmedName = name.trim(); // Fixed: use new variable
            Artist artist = artistRepository.findByName(trimmedName)
                    .orElseGet(() -> {
                        Artist newArtist = new Artist();
                        newArtist.setName(trimmedName);
                        return artistRepository.save(newArtist);
                    });
            artists.add(artist);
        }
        song.setArtists(artists);

        // Process Genres
        String genreName = getTagValue(tag, FieldKey.GENRE, "Unknown");
        List<Genre> genres = new ArrayList<>();
        // Fixed: changed regex from ",|;" to "[,;]"
        for (String name : genreName.split("[,;]")) {
            String trimmedName = name.trim(); // Fixed: use new variable
            Genre genre = genreRepository.findByName(trimmedName)
                    .orElseGet(() -> {
                        Genre newGenre = new Genre();
                        newGenre.setName(trimmedName);
                        return genreRepository.save(newGenre);
                    });
            genres.add(genre);
        }
        song.setGenres(genres);

        // Save song
        Song savedSong = songRepository.save(song);
        System.out.println("Imported: " + savedSong.getTitle());

        return savedSong;
    }

    private String getTagValue(Tag tag, FieldKey key, String defaultValue) {
        try {
            String value = tag.getFirst(key);
            return (value == null || value.isBlank()) ? defaultValue : value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1).toUpperCase() : "UNKNOWN";
    }

    // These methods are for future use
    public List<Song> getAllSongs() {
        return songRepository.findAll();
    }

    public Optional<Song> getSongById(Long id) {
        return songRepository.findById(id);
    }
}