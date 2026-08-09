package com.sebastiandorata.musicdashboard.service.Import;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-side accessor for the song library.
 *
 * <p>This class used to also handle writing new songs to the database
 * during import ({@code importSong}, {@code importSongWithOverrides},
 * {@code extractMetadataPreview} — the two methods your IDE flagged as
 * "never used" lived here). That responsibility now belongs to
 * {@link SongMetadataExtractor} (reads the file) and {@link SongUpdateService}
 * (persists the result), so the import pipeline has exactly one owner and
 * one pass per file.
 *
 * <p>This class is kept only for {@link #getAllSongs()}, which several
 * read-only views — {@code DashboardController}, {@code MyLibraryController},
 * {@code RecentlyPlayedController}, {@code LibraryService} — depend on.
 * Nothing about those call sites needs to change.
 */
@Service
public class SongImportService {

    @Autowired private SongRepository songRepository;

    public List<Song> getAllSongs() {
        return songRepository.findAllWithArtistsAndGenres();
    }
}