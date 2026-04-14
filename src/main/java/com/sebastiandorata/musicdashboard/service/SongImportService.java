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
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

/**
 * Reads audio metadata from MP3 and M4A files and persists them to the database.
 *
 * <p>Uses jAudioTagger to extract title, album, artist(s), genre(s), track
 * number, and embedded artwork. Deduplicates songs by file path, albums and
 * artists by title/name, and saves album art as {@code cover.jpg} alongside
 * the audio files. Multi-artist and multi-genre tags are split on
 * {@code ,}, {@code ;}, and {@code &}.</p>
 */
@Service
public class SongImportService {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private GenreRepository genreRepository;

    private String stripFileExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return filename;
        }

        String[] commonAudioExtensions = {".mp3", ".m4a", ".wav", ".flac", ".ogg", ".aac", ".wma"};
        String lowerName = filename.toLowerCase();

        for (String ext : commonAudioExtensions) {
            if (lowerName.endsWith(ext)) {
                return filename.substring(0, filename.length() - ext.length());
            }
        }
        return filename;
    }

    @Transactional
    public Song importSong(File file) throws Exception {
        Optional<Song> existingSong = songRepository.findByFilePath(file.getAbsolutePath());
        if (existingSong.isPresent()) {
            System.out.println("Song already imported: " + file.getName());
            return existingSong.get();
        }

        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTag();

        Song song = new Song();
        song.setFilePath(file.getAbsolutePath());
        song.setTitle(stripFileExtension(getTagValue(tag, FieldKey.TITLE, file.getName())));
        song.setDuration(audioFile.getAudioHeader().getTrackLength());
        song.setFileFormat(getFileExtension(file));
        song.setFileSizeBytes(file.length());
        song.setDateFirstListened(LocalDate.now());
        song.setListenCount(0);
        song.setBitRate((int) audioFile.getAudioHeader().getBitRateAsNumber());
        song.setSampleRate(audioFile.getAudioHeader().getSampleRateAsNumber());
        song.setChannels(parseChannels(audioFile.getAudioHeader().getChannels()));
        song.setCodec(audioFile.getAudioHeader().getEncodingType());

        // Track number
        song.setTrackNum(parseTrackNumber(getTagValue(tag, FieldKey.TRACK, null)));

        // Single-song / no-album rule to use song title as album name.
        String rawAlbum = getTagValue(tag, FieldKey.ALBUM, null);
        String albumTitle;
        Integer trackNum;

        if (rawAlbum == null || rawAlbum.isBlank() || rawAlbum.equalsIgnoreCase("Unknown Album")) {
            albumTitle = song.getTitle(); // use song title as album name
            trackNum   = 1;
            song.setTrackNum(1);
        } else {
            albumTitle = rawAlbum;
            trackNum   = parseTrackNumber(getTagValue(tag, FieldKey.TRACK, null));
            song.setTrackNum(trackNum);
        }
        //

        String yearStr = getTagValue(tag, FieldKey.YEAR, null);
        Album album = albumRepository.findByTitle(albumTitle)
                .orElseGet(() -> {
                    Album newAlbum = new Album();
                    newAlbum.setTitle(albumTitle);
                    if (yearStr != null) {
                        try { newAlbum.setReleaseYear(Integer.parseInt(yearStr)); }
                        catch (NumberFormatException ignored) {}
                    }
                    return albumRepository.save(newAlbum);
                });
        song.setAlbum(album);

        String artPath = extractAndSaveAlbumArt(audioFile, file.getAbsolutePath());
        if (artPath != null && album.getAlbumArtPath() == null) {
            album.setAlbumArtPath(artPath);
            albumRepository.save(album);
        }

        // Artists
        String artistName = getTagValue(tag, FieldKey.ARTIST, "Unknown Artist");
        Set<Artist> artists = resolveArtists(artistName);
        song.setArtists(artists);
        for (Artist artist : artists) {
            if (!album.getArtists().contains(artist)) {
                album.getArtists().add(artist);
            }
        }
        albumRepository.save(album);

        // Genres
        String genreName = getTagValue(tag, FieldKey.GENRE, "Unknown");
        song.setGenres(resolveGenres(genreName));

        Song savedSong = songRepository.save(song);
        System.out.println("Imported: " + savedSong.getTitle());
        return savedSong;
    }

    private Integer parseTrackNumber(String trackStr) {
        if (trackStr == null || trackStr.isBlank()) return null;
        try {
            int parsed = Integer.parseInt(trackStr.split("/")[0].trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Set<Artist> resolveArtists(String raw) {
        Set<Artist> set = new HashSet<>();
        for (String name : raw.split("[,;&]")) {
            String t = name.trim();
            if (t.isBlank()) continue;
            set.add(artistRepository.findByName(t).orElseGet(() -> {
                Artist a = new Artist();
                a.setName(t);
                return artistRepository.save(a);
            }));
        }
        return set;
    }

    private Set<Genre> resolveGenres(String raw) {
        Set<Genre> set = new HashSet<>();
        for (String name : raw.split("[,;]")) {
            String t = name.trim();
            if (t.isBlank()) continue;
            set.add(genreRepository.findByName(t).orElseGet(() -> {
                Genre g = new Genre();
                g.setName(t);
                return genreRepository.save(g);
            }));
        }
        return set;
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
        return songRepository.findAllWithArtistsAndGenres();
    }


}