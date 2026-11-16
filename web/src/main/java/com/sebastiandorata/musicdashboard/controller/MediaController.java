package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.AlbumRepository;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.time.Instant;
import java.util.Locale;

/**
 * Streams song audio files and album cover images from disk to the browser.
 *
 * <p>Both endpoints return a {@link Resource} body, which lets Spring MVC answer HTTP
 * {@code Range} requests with {@code 206 Partial Content}; the browser's audio element needs this
 * for seeking. File paths come only from the database ({@code Song.filePath},
 * {@code Album.albumArtPath}), never from the request.</p>
 *
 * <p>SRP: only serves media bytes; queue logic lives in {@code PlayerController}.</p>
 *
 * <p>Time Complexity: O(1) per request (the file is streamed lazily).</p>
 * <p>Space Complexity: O(1)</p>
 */
@RestController
public class MediaController {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private AlbumRepository albumRepository;

    /**
     * Streams the audio file of a song.
     *
     * @param songId id of the song
     * @return the audio file with its content type and {@code Accept-Ranges: bytes}
     * @throws ResponseStatusException 404 when the song or its file does not exist
     */
    @GetMapping("/media/song/{songId}")
    public ResponseEntity<Resource> song(@PathVariable Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found"));
        File file = existingFile(song.getFilePath(), "Song file not found");

        return ResponseEntity.ok()
                .contentType(audioType(file.getName()))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .lastModified(Instant.ofEpochMilli(file.lastModified()))
                .body(new FileSystemResource(file));
    }

    /**
     * Serves the cover image of an album.
     *
     * @param albumId id of the album
     * @return the image file with its content type and a {@code Last-Modified} header
     * @throws ResponseStatusException 404 when the album has no art path or the file is missing
     */
    @GetMapping("/media/art/album/{albumId}")
    public ResponseEntity<Resource> albumArt(@PathVariable Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));
        File file = existingFile(album.getAlbumArtPath(), "Album art not found");

        return ResponseEntity.ok()
                .contentType(imageType(file.getName()))
                .lastModified(Instant.ofEpochMilli(file.lastModified()))
                .body(new FileSystemResource(file));
    }

    /**
     * Resolves a stored path to an existing regular file.
     *
     * @param path    path from the database, may be {@code null}
     * @param message 404 reason when the file cannot be used
     * @return the file
     * @throws ResponseStatusException 404 when the path is empty or the file is missing
     */
    private File existingFile(String path, String message) {
        if (path == null || path.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
        File file = new File(path);
        if (!file.isFile()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
        return file;
    }

    /**
     * Maps an audio file name to its media type by extension.
     *
     * @param name file name
     * @return the media type; {@code application/octet-stream} when unknown
     */
    private MediaType audioType(String name) {
        return switch (extension(name)) {
            case "mp3" -> MediaType.parseMediaType("audio/mpeg");
            case "m4a", "mp4", "aac" -> MediaType.parseMediaType("audio/mp4");
            case "flac" -> MediaType.parseMediaType("audio/flac");
            case "wav" -> MediaType.parseMediaType("audio/wav");
            case "ogg", "oga" -> MediaType.parseMediaType("audio/ogg");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    /**
     * Maps an image file name to its media type by extension.
     *
     * @param name file name
     * @return the media type; {@code application/octet-stream} when unknown
     */
    private MediaType imageType(String name) {
        return switch (extension(name)) {
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    /**
     * Returns the lower-case extension of a file name without the dot.
     *
     * @param name file name
     * @return the extension, or an empty string
     */
    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
