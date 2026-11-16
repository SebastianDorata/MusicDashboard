package com.sebastiandorata.musicdashboard.dto.lastfm;

/**
 * One scrobble entry from Last.fm's {@code user.getRecentTracks} response,
 * flattened from the raw JSON (see {@code recenttracks[].track[]}) into a
 * single record per play.
 *
 * <p>{@code uts} is the Unix timestamp Last.fm recorded the play at, and is
 * the source value for {@code PlaybackHistory.externalScrobbleId} — used to
 * detect and skip already-imported scrobbles on a re-import.
 *
 * @param artist     resolved artist reference (mbid may be blank)
 * @param album      resolved album reference (mbid may be blank)
 * @param trackName  track title as scrobbled
 * @param trackMbid  MusicBrainz recording ID, or blank/null if not supplied
 * @param uts        Unix timestamp (seconds) the scrobble was recorded at
 */
public record LastFmScrobble(
        LastFmArtistRef artist,
        LastFmAlbumRef album,
        String trackName,
        String trackMbid,
        long uts
) {}