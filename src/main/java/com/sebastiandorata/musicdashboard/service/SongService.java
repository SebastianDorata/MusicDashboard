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
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        song.setChannels(parseChannels(audioFile.getAudioHeader().getChannels()));
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
            String artPath = extractAndSaveAlbumArt(audioFile, file.getAbsolutePath());
            if (artPath != null && album.getAlbumArtPath() == null) {
                album.setAlbumArtPath(artPath);
                albumRepository.save(album);  // update album with art path
        }

        // Process Artists
        String artistName = getTagValue(tag, FieldKey.ARTIST, "Unknown Artist");
        List<Artist> artists = new ArrayList<>();

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
    private String extractAndSaveAlbumArt(AudioFile audioFile, String songFilePath) {
        try {
            Tag tag = audioFile.getTag();
            if (tag == null) return null;

            Artwork artwork = tag.getFirstArtwork();
            if (artwork == null) return null;

            byte[] imageData = artwork.getBinaryData();
            if (imageData == null || imageData.length == 0) return null;

            // Always name it cover.jpg in the album's folder & one file per album,
            String folder  = new File(songFilePath).getParent();
            String artPath = folder + File.separator + "cover.jpg";

            // Only write to disk if the file doesn't already exist and avoids redundant writes when importing multiple songs from the same album
            File artFile = new File(artPath);
                if (!artFile.exists()) {
                    Files.write(Paths.get(artPath), imageData);
                    System.out.println("Album art saved: " + artPath);
                }

            return artPath;

        } catch (Exception e) {
            System.out.println("Could not extract album art: " + e.getMessage());
            return null;
        }
    }
    private int parseChannels(String channelStr) {
        if (channelStr == null) return 2; // default to stereo
        try {
            return Integer.parseInt(channelStr.trim());
        } catch (NumberFormatException ignored) {}

        // Handle descriptive strings
        return switch (channelStr.trim().toLowerCase()) {
            case "mono"-> 1;
            case "stereo", "joint stereo", "dual channel", "joint_stereo"-> 2;
            case "5.1", "surround"-> 6;
            default -> {
                System.out.println("Unknown channel format: '" + channelStr + "', defaulting to 2");
                yield 2;
            }
        };
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


    public List<Song> getAllSongs() {
        return songRepository.findAll();
    }

    public Optional<Song> getSongById(Long id) {
        return songRepository.findById(id);
    }
}