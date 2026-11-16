/*
 * player.js - browser side of the music player (window.MD.player).
 *
 * The browser plays audio with one shared HTML5 Audio element; the server (PlayerController
 * and the session-scoped MusicPlayerService) owns the queue, next/previous and play tracking.
 * This script:
 *   - exposes MD.player.{playSong, setQueue, playQueue, playFromLibrary, next, previous, toggle, seek, state};
 *   - binds every [data-player] root on the page (fragments/player.html: "large" and "bar");
 *   - keeps sliders and time labels in step with the audio;
 *   - reports 20 seconds of actual playback (POST /api/player/tracked) and song ends (POST /api/player/ended);
 *   - dispatches 'md:songchange' (detail: SongView|null) and 'md:playstate' (detail: {playing}) on document;
 *   - survives page loads (this is a multi-page app): {songId, position, playing, played} live in
 *     sessionStorage and are restored after GET /api/player/state.
 *
 * Loaded after app.js; uses MD.api, MD.toast and MD.fmt.
 */
(function () {
    'use strict';

    window.MD = window.MD || {};

    var STORAGE_KEY = 'md.player';
    var TRACK_SECONDS = 20;
    var SAVE_INTERVAL_MS = 1000;

    var audio = new Audio();
    audio.preload = 'auto';

    /** Public state object: {song: SongView|null, playing: boolean}. */
    var state = { song: null, playing: false };

    /** Bound panels: [{root, refs}]. */
    var panels = [];

    var serverPlaying = false;   // last known server-side playing flag
    var switching = false;       // true while a song is being loaded by us
    var dragging = false;        // a slider is being dragged
    var lastTime = 0;            // previous currentTime, for accumulating real playback
    var played = 0;              // seconds actually played of the current song
    var tracked = false;         // /tracked already sent for the current song
    var lastSave = 0;
    var ending = false;          // /ended in flight

    /**
     * Formats seconds as m:ss, using MD.fmt when available.
     * @param {number} seconds
     * @returns {string}
     */
    function fmtTime(seconds) {
        if (window.MD.fmt && MD.fmt.time) return MD.fmt.time(seconds);
        var s = Math.max(0, Math.floor(seconds || 0));
        return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0');
    }

    /**
     * Shows an error toast, falling back to the console.
     * @param {string} message
     */
    function toastError(message) {
        if (window.MD.toast) MD.toast(message, 'error');
        else console.error(message);
    }

    /**
     * Reads the persisted player snapshot from sessionStorage.
     * @returns {{songId:number, position:number, playing:boolean, played:number, tracked:boolean}|null}
     */
    function readSaved() {
        try {
            var raw = sessionStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    /**
     * Writes the current player snapshot to sessionStorage (or clears it when no song is loaded).
     */
    function save() {
        lastSave = Date.now();
        try {
            if (!state.song) {
                sessionStorage.removeItem(STORAGE_KEY);
                return;
            }
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
                songId: state.song.id,
                position: audio.currentTime || 0,
                playing: state.playing,
                played: played,
                tracked: tracked
            }));
        } catch (e) { /* storage unavailable: continuity is best effort */ }
    }

    /**
     * Duration of the current song in seconds (audio metadata first, then the server's value).
     * @returns {number}
     */
    function totalSeconds() {
        if (isFinite(audio.duration) && audio.duration > 0) return audio.duration;
        return (state.song && state.song.duration) || 0;
    }

    // ───────────────────────── rendering ─────────────────────────

    /**
     * Calls fn(refs, root) for every bound panel.
     * @param {function(Object, HTMLElement):void} fn
     */
    function eachPanel(fn) {
        panels.forEach(function (p) { fn(p.refs, p.root); });
    }

    /**
     * Updates the title, artist link, album art and slider range of all panels for a song.
     * Shows the empty state ("No song playing" / "—") when song is null.
     * @param {Object|null} song SongView
     */
    function renderSong(song) {
        eachPanel(function (r, root) {
            r.title.textContent = song ? song.title : 'No song playing';
            r.title.title = song ? song.title : '';
            if (song) {
                r.artist.textContent = song.artistName || 'Unknown Artist';
                if (song.artistId != null) {
                    r.artist.href = '/library?artist=' + encodeURIComponent(song.artistId);
                    r.artist.classList.add('Cursor-Hand');
                } else {
                    r.artist.removeAttribute('href');
                    r.artist.classList.remove('Cursor-Hand');
                }
            } else {
                r.artist.textContent = '—';
                r.artist.removeAttribute('href');
                r.artist.classList.remove('Cursor-Hand');
            }
            if (r.img) {
                if (song && song.artUrl) {
                    r.img.hidden = false;
                    r.art.classList.add('has-art');
                    r.img.src = song.artUrl;
                } else {
                    r.img.hidden = true;
                    r.img.removeAttribute('src');
                    r.art.classList.remove('has-art');
                }
            }
            if (r.slider) {
                r.slider.max = song ? (song.duration || 100) : 100;
                r.slider.value = 0;
                r.slider.style.setProperty('--fill', '0%');
            }
            if (r.current) r.current.textContent = '0:00';
            if (r.remaining) r.remaining.textContent = '-' + fmtTime(song ? song.duration : 0);
        });
        renderPlaying();
    }

    /**
     * Updates sliders and time labels from the audio position. Skipped while a slider is dragged.
     */
    function renderTime() {
        if (dragging) return;
        var cur = audio.currentTime || 0;
        var total = totalSeconds();
        eachPanel(function (r) {
            if (r.slider) {
                if (total && Number(r.slider.max) !== total) r.slider.max = total;
                r.slider.value = cur;
                r.slider.style.setProperty('--fill', total ? Math.min(100, cur / total * 100) + '%' : '0%');
            }
            if (r.current) r.current.textContent = fmtTime(cur);
            if (r.remaining) r.remaining.textContent = '-' + fmtTime(total - cur);
        });
    }

    /**
     * Updates the play/pause icon, aria label and the is-playing class of all panels.
     */
    function renderPlaying() {
        eachPanel(function (r, root) {
            var on = !!(state.song && state.playing);
            root.classList.toggle('is-playing', on);
            if (r.playIcon) r.playIcon.src = on ? '/icons/pause.png' : '/icons/play.png';
            if (r.play) r.play.setAttribute('aria-label', on ? 'Pause' : 'Play');
        });
    }

    // ───────────────────────── state changes ─────────────────────────

    /**
     * Sets the playing flag, re-renders and notifies listeners when it changed.
     * @param {boolean} playing
     */
    function setPlaying(playing) {
        if (state.playing === playing) return;
        state.playing = playing;
        renderPlaying();
        document.dispatchEvent(new CustomEvent('md:playstate', { detail: { playing: playing } }));
    }

    /**
     * Tells the server the playing flag changed (pause/resume), only when it differs from what it knows.
     * @param {boolean} playing
     */
    function syncServer(playing) {
        if (serverPlaying === playing) return;
        serverPlaying = playing;
        MD.api.post(playing ? '/api/player/resume' : '/api/player/pause').catch(function () { });
    }

    /**
     * Loads a song into the audio element and updates the UI. Does not start playback.
     * @param {Object|null} song SongView, or null to clear the player
     * @param {number} [startAt=0] position in seconds to seek to once metadata is ready
     */
    function loadSong(song, startAt) {
        switching = true;
        state.song = song;
        played = 0;
        tracked = false;
        ending = false;
        lastTime = startAt || 0;
        if (song) {
            audio.src = song.streamUrl || ('/media/song/' + song.id);
            if (startAt > 0) {
                audio.addEventListener('loadedmetadata', function seekOnce() {
                    audio.removeEventListener('loadedmetadata', seekOnce);
                    audio.currentTime = startAt;
                    lastTime = startAt;
                    renderTime();
                });
            }
        } else {
            audio.removeAttribute('src');
            audio.load();
            setPlaying(false);
        }
        renderSong(song);
        if (startAt > 0) {
            eachPanel(function (r) {
                if (r.current) r.current.textContent = fmtTime(startAt);
                if (r.slider) r.slider.value = startAt;
            });
        }
        switching = false;
        // fired even when the same song is loaded again (e.g. single-song queue repeat)
        document.dispatchEvent(new CustomEvent('md:songchange', { detail: song }));
        save();
    }

    /**
     * Starts audio playback; falls back silently to the paused UI when the browser blocks autoplay.
     * @param {boolean} [quiet=false] true to swallow autoplay-policy errors without a toast
     * @returns {Promise<void>}
     */
    function startAudio(quiet) {
        var p = audio.play();
        if (!p || !p.then) return Promise.resolve();
        return p.catch(function (err) {
            setPlaying(false);
            if (!quiet && err && err.name !== 'AbortError' && err.name !== 'NotAllowedError') {
                toastError('Unable to play this song');
            }
        });
    }

    /**
     * Applies a PlayerState returned by the server: loads its song and starts playing it.
     * @param {{song:Object|null, playing:boolean, restart:boolean}} s
     * @returns {Promise<void>}
     */
    function applyAndPlay(s) {
        serverPlaying = !!(s && s.playing);
        if (!s || !s.song) {
            if (state.song) loadSong(null);
            return Promise.resolve();
        }
        loadSong(s.song, 0);
        return startAudio(false);
    }

    /**
     * Reports an API failure to the user (the reason is the desktop's error text for unplayable songs).
     * @param {Error} err
     */
    function fail(err) {
        toastError(err && err.message ? err.message : 'Playback failed');
    }

    /**
     * Current audio position for the server's skip-tracking rule, or null when nothing is loaded.
     * @returns {number|null}
     */
    function position() {
        return state.song ? (audio.currentTime || 0) : null;
    }

    // ───────────────────────── public API ─────────────────────────

    /**
     * Plays one song (desktop musicPlayerService.playSong).
     * @param {number} songId
     * @returns {Promise<void>}
     */
    function playSong(songId) {
        return MD.api.post('/api/player/play', { songId: songId, position: position() })
            .then(applyAndPlay).catch(fail);
    }

    /**
     * Replaces the queue with a shuffled copy of the songs (desktop musicPlayerService.setQueue).
     * @param {number[]} songIds
     * @returns {Promise<void>}
     */
    function setQueue(songIds) {
        return MD.api.post('/api/player/queue', { songIds: songIds }).then(function () { }).catch(fail);
    }

    /**
     * setQueue(songIds) followed by playSong(startSongId).
     * @param {number[]} songIds
     * @param {number} startSongId
     * @returns {Promise<void>}
     */
    function playQueue(songIds, startSongId) {
        return MD.api.post('/api/player/queue', { songIds: songIds })
            .then(function () { return playSong(startSongId); }).catch(fail);
    }

    /**
     * Queues the whole library (shuffled, like setQueue) and then plays one song.
     * Desktop: the Recently Played panel's setQueue(allSongs) + playSong(song).
     * @param {number} songId
     * @returns {Promise<void>}
     */
    function playFromLibrary(songId) {
        return MD.api.post('/api/player/queue/library')
            .then(function () { return playSong(songId); }).catch(fail);
    }

    /**
     * Skips to the next song of the queue (wraps around).
     * @returns {Promise<void>}
     */
    function next() {
        return MD.api.post('/api/player/next', { position: position() }).then(function (s) {
            if (s && s.queueSize === 0) return; // empty queue: desktop playNext does nothing
            return applyAndPlay(s);
        }).catch(fail);
    }

    /**
     * Goes to the previous song, or restarts the current one when it is more than 3 seconds in.
     * @returns {Promise<void>}
     */
    function previous() {
        return MD.api.post('/api/player/previous', { position: position() }).then(function (s) {
            if (s && s.restart) {
                audio.currentTime = 0;
                lastTime = 0;
                renderTime();
                return;
            }
            if (s && s.queueSize === 0) return;
            return applyAndPlay(s);
        }).catch(fail);
    }

    /**
     * Toggles between play and pause. Does nothing when no song is loaded.
     */
    function toggle() {
        if (!state.song) return;
        if (audio.paused) startAudio(false);
        else audio.pause();
    }

    /**
     * Seeks the current song.
     * @param {number} seconds
     */
    function seek(seconds) {
        if (!state.song) return;
        audio.currentTime = Math.max(0, seconds);
        lastTime = audio.currentTime;
        renderTime();
        save();
    }

    window.MD.player = {
        playSong: playSong,
        setQueue: setQueue,
        playQueue: playQueue,
        playFromLibrary: playFromLibrary,
        next: next,
        previous: previous,
        toggle: toggle,
        seek: seek,
        get state() { return state; }
    };

    // ───────────────────────── audio events ─────────────────────────

    audio.addEventListener('timeupdate', function () {
        var t = audio.currentTime || 0;
        // count only real, continuous playback (ignore seeks and jumps)
        if (!audio.paused && !audio.seeking) {
            var d = t - lastTime;
            if (d > 0 && d < 2) played += d;
        }
        lastTime = t;
        if (state.song && !tracked && played >= TRACK_SECONDS) {
            tracked = true;
            MD.api.post('/api/player/tracked').catch(function () { });
        }
        renderTime();
        if (Date.now() - lastSave >= SAVE_INTERVAL_MS) save();
    });

    audio.addEventListener('seeked', function () { lastTime = audio.currentTime || 0; });
    audio.addEventListener('loadedmetadata', renderTime);
    audio.addEventListener('durationchange', renderTime);

    audio.addEventListener('play', function () {
        if (switching || !state.song) return;
        setPlaying(true);
        syncServer(true);
    });

    audio.addEventListener('pause', function () {
        if (switching || audio.ended || !state.song) return;
        setPlaying(false);
        syncServer(false);
        save();
    });

    audio.addEventListener('ended', function () {
        if (!state.song || ending) return;
        ending = true;
        var duration = isFinite(audio.duration) ? audio.duration : (state.song.duration || 0);
        MD.api.post('/api/player/ended', { duration: Math.round(duration) })
            .then(function (s) {
                if (!s || !s.song) { setPlaying(false); return; } // empty queue: stop at the end
                return applyAndPlay(s);
            })
            .catch(function (err) { setPlaying(false); fail(err); });
    });

    audio.addEventListener('error', function () {
        if (!state.song || !audio.error) return;
        toastError('Unable to play "' + state.song.title + '"');
        setPlaying(false);
    });

    window.addEventListener('pagehide', save);
    document.addEventListener('visibilitychange', function () {
        if (document.visibilityState === 'hidden') save();
    });

    // ───────────────────────── panel binding ─────────────────────────

    /**
     * Collects the data-role elements of a panel root.
     * @param {HTMLElement} root a [data-player] element
     * @returns {Object} element references by role
     */
    function collectRefs(root) {
        function q(role) { return root.querySelector('[data-role="' + role + '"]'); }
        return {
            art: q('art'), img: q('art-img'), title: q('title'), artist: q('artist'),
            prev: q('prev'), play: q('play'), playIcon: q('play-icon'), next: q('next'),
            slider: q('slider'), current: q('current'), remaining: q('remaining')
        };
    }

    /**
     * Wires one panel's buttons, slider and artwork handlers.
     * @param {HTMLElement} root a [data-player] element
     */
    function bindPanel(root) {
        var r = collectRefs(root);
        panels.push({ root: root, refs: r });

        if (r.prev) r.prev.addEventListener('click', previous);
        if (r.next) r.next.addEventListener('click', next);
        if (r.play) r.play.addEventListener('click', toggle);

        if (r.img) {
            r.img.addEventListener('error', function () {
                r.img.hidden = true;
                r.art.classList.remove('has-art');
            });
        }

        if (r.slider) {
            var s = r.slider;
            var startDrag = function () { dragging = true; };
            s.addEventListener('pointerdown', startDrag);
            s.addEventListener('keydown', startDrag);
            s.addEventListener('input', function () {
                var v = Number(s.value);
                var total = Number(s.max);
                s.style.setProperty('--fill', total ? Math.min(100, v / total * 100) + '%' : '0%');
                eachPanel(function (o) {
                    if (o.current) o.current.textContent = fmtTime(v);
                    if (o.remaining) o.remaining.textContent = '-' + fmtTime(total - v);
                });
            });
            s.addEventListener('change', function () {
                dragging = false;
                seek(Math.round(Number(s.value)));
            });
            window.addEventListener('pointerup', function () {
                // a drag that ended without a change event (value untouched)
                if (dragging) { dragging = false; renderTime(); }
            });
        }
    }

    /**
     * Binds all panels and restores the player from the server and sessionStorage.
     */
    function init() {
        document.querySelectorAll('[data-player]').forEach(bindPanel);
        renderSong(null);

        MD.api.get('/api/player/state').then(function (s) {
            if (!s || !s.song) return;
            serverPlaying = !!s.playing;
            var saved = readSaved();
            var same = saved && saved.songId === s.song.id;
            var wantPlay = same ? !!saved.playing : !!s.playing;

            loadSong(s.song, same ? saved.position : 0);
            if (same) {
                // the 20 second rule must not restart or double-fire after a page load
                played = saved.played || 0;
                tracked = !!saved.tracked;
            }
            // if the browser blocks autoplay, startAudio falls back to the paused UI without a toast
            if (wantPlay) startAudio(true);
        }).catch(function () { /* not logged in or server unavailable: stay in the empty state */ });
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();
})();
