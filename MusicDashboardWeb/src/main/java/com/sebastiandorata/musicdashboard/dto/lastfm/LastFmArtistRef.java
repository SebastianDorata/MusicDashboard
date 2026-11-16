package com.sebastiandorata.musicdashboard.dto.lastfm;

/**
 * Raw artist reference as returned inside a Last.fm scrobble.
 *
 * <p>Mirrors the {@code artist} object in Last.fm's {@code user.getRecentTracks}
 * JSON response: {@code {"mbid": "...", "#text": "..."}}. Kept as a plain,
 * unvalidated carrier of whatever Last.fm sent — {@code mbid} is frequently
 * blank and {@code name} should never be assumed non-null-safe by callers.
 *
 * @param mbid MusicBrainz artist ID, or blank/null if Last.fm didn't supply one
 * @param name display name as scrobbled (e.g. "Paramore")
 */
public record LastFmArtistRef(String mbid, String name) {}

