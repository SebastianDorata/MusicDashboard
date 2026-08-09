package com.sebastiandorata.musicdashboard.dto.lastfm;

/**
 * Raw album reference as returned inside a Last.fm scrobble.
 *
 * <p>Mirrors the {@code album} object in Last.fm's {@code user.getRecentTracks}
 * JSON response: {@code {"mbid": "...", "#text": "..."}}.
 *
 * @param mbid MusicBrainz release-group ID, or blank/null if not supplied
 * @param name album title as scrobbled
 */
public record LastFmAlbumRef(String mbid, String name) {}
