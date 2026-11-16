/*
 * dashboard.js - behaviour of the My Dashboard page (desktop DashboardController,
 * CardFactory stat cards, RecentlyPlayedController, TopArtistsController,
 * DashboardGraphController, DashboardLineChartBuilder and SmoothLineChart).
 *
 *   - draws the listening chart as inline SVG (Catmull-Rom smoothing, tension 0.9, fixed 0-750 hour axis,
 *     month labels thinned to every other one on a narrow plot, per-series colours) and redraws it when
 *     the window is resized or the year changes;
 *   - fills the colour-coded year title row and the "All Time Stats" figures beside the chart;
 *   - wires the click behaviour: stat-card song label (click = open album, double-click = play),
 *     album label (open album), recently-played row (double-click = play, single tap on touch),
 *     artist row (open artist);
 *   - refreshes stat cards, recently played and top artists from /api/dashboard/** when the playing
 *     song changes (md:songchange), once right away and once ~21 s later when the play has been recorded.
 *
 * Depends on app.js (MD.api, MD.el, MD.toast) and player.js (MD.player, md:songchange).
 */
(function () {
    'use strict';

    const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const Y_MAX = 750;
    const Y_TICK = 50;
    const TENSION = 0.9;
    const SVG_NS = 'http://www.w3.org/2000/svg';
    const COLOUR_INDEX = {current: 0, previous: 1, highest: 2};
    const RECORD_DELAY_MS = 21000;
    const REFRESH_DELAY_MS = 300;
    const SINGLE_CLICK_DELAY_MS = 300;

    /** The last chart payload, kept so a resize can redraw without refetching. */
    let lastChart = null;
    /** Timers of the pending post-song-change refreshes. */
    let pendingTimers = [];

    /* ─── Small helpers ─────────────────────────────────────── */

    /**
     * Shorthand for document.getElementById.
     * @param {string} id the element id
     * @returns {HTMLElement|null} the element
     */
    function byId(id) {
        return document.getElementById(id);
    }

    /**
     * Formats an integer with thousands separators like the desktop NumberFormat (US locale).
     * @param {number} n the number
     * @returns {string} the formatted number
     */
    function fmtInt(n) {
        return Number(n).toLocaleString('en-US');
    }

    /**
     * Navigates to My Library drilled into an album.
     * @param {number|string} albumId the album id
     */
    function openAlbum(albumId) {
        window.location.href = '/library?album=' + encodeURIComponent(albumId);
    }

    /**
     * Navigates to My Library drilled into an artist.
     * @param {number|string} artistId the artist id
     */
    function openArtist(artistId) {
        window.location.href = '/library?artist=' + encodeURIComponent(artistId);
    }

    /**
     * Creates an SVG element with attributes.
     * @param {string} tag the SVG tag name
     * @param {Object<string,string|number>} attrs attributes to set
     * @returns {SVGElement} the element
     */
    function svg(tag, attrs) {
        const node = document.createElementNS(SVG_NS, tag);
        Object.keys(attrs || {}).forEach(function (k) {
            node.setAttribute(k, attrs[k]);
        });
        return node;
    }

    /* ─── Chart ─────────────────────────────────────────────── */

    /**
     * Builds the SVG path data for a smooth curve through the points, using the same
     * tension-scaled Catmull-Rom to cubic Bezier conversion as the desktop SmoothLineChart
     * (end points use themselves as the missing neighbour, so the curve does not bend away at the edges).
     * @param {Array<[number,number]>} pts the points in pixel space, left to right
     * @returns {string} the path "d" attribute; empty for fewer than two points
     */
    function smoothPath(pts) {
        if (pts.length < 2) return '';
        const last = pts.length - 1;
        let d = 'M' + pts[0][0] + ',' + pts[0][1];
        for (let i = 0; i < last; i++) {
            const p0 = i === 0 ? pts[0] : pts[i - 1];
            const p1 = pts[i];
            const p2 = pts[i + 1];
            const p3 = i + 1 === last ? pts[last] : pts[i + 2];
            const c1x = p1[0] + TENSION * (p2[0] - p0[0]) / 6;
            const c1y = p1[1] + TENSION * (p2[1] - p0[1]) / 6;
            const c2x = p2[0] - TENSION * (p3[0] - p1[0]) / 6;
            const c2y = p2[1] - TENSION * (p3[1] - p1[1]) / 6;
            d += ' C' + c1x + ',' + c1y + ' ' + c2x + ',' + c2y + ' ' + p2[0] + ',' + p2[1];
        }
        return d;
    }

    /**
     * Draws the line chart into #lineChart from a chart payload.
     * @param {Object} data the /api/dashboard/chart response
     */
    function drawChart(data) {
        const box = byId('lineChart');
        if (!box || !data) return;
        box.innerHTML = '';
        const w = Math.max(box.clientWidth, 200);
        const h = Math.max(box.clientHeight, 140);
        const m = {left: 46, right: 18, top: 14, bottom: 30};
        const pw = w - m.left - m.right;
        const ph = h - m.top - m.bottom;

        const root = svg('svg', {viewBox: '0 0 ' + w + ' ' + h, preserveAspectRatio: 'none'});
        root.appendChild(svg('rect', {
            'class': 'chart-plot-background', x: m.left, y: m.top, width: pw, height: ph
        }));

        const yOf = function (v) { return m.top + ph - (Math.min(v, Y_MAX) / Y_MAX) * ph; };
        const xOf = function (i) { return m.left + ((i + 0.5) / 12) * pw; };

        // Horizontal grid and y labels (labels thinned out when the chart is short)
        const ticks = Y_MAX / Y_TICK;
        const labelEvery = Math.max(1, Math.ceil(16 / (ph / ticks)));
        const grid = svg('g', {'class': 'chart-horizontal-grid-lines'});
        const yAxis = svg('g', {'class': 'axis'});
        for (let t = 0; t <= ticks; t++) {
            const y = yOf(t * Y_TICK);
            grid.appendChild(svg('line', {x1: m.left, x2: m.left + pw, y1: y, y2: y}));
            if (t % labelEvery === 0) {
                const label = svg('text', {x: m.left - 8, y: y + 4, 'text-anchor': 'end'});
                label.textContent = t * Y_TICK;
                yAxis.appendChild(label);
            }
        }
        root.appendChild(grid);
        root.appendChild(yAxis);

        // Month labels
        const xAxis = svg('g', {'class': 'axis'});
        const monthEvery = pw / 12 < 30 ? 2 : 1; // thin the labels when the plot is narrow
        MONTHS.forEach(function (name, i) {
            if (i % monthEvery !== 0) return;
            const label = svg('text', {x: xOf(i), y: m.top + ph + 20, 'text-anchor': 'middle'});
            label.textContent = name;
            xAxis.appendChild(label);
        });
        root.appendChild(xAxis);

        // Series: months without data are skipped and the line joins the remaining points
        let anyPoint = false;
        data.series.forEach(function (s) {
            const colour = 'default-color' + (COLOUR_INDEX[s.role] != null ? COLOUR_INDEX[s.role] : 0);
            const pts = [];
            s.points.forEach(function (v, i) {
                if (v != null) pts.push([xOf(i), yOf(v), i, v]);
            });
            if (!pts.length) return;
            anyPoint = true;
            const group = svg('g', {'class': colour});
            group.appendChild(svg('path', {
                'class': 'chart-series-line ' + colour,
                d: smoothPath(pts.map(function (p) { return [p[0], p[1]]; }))
            }));
            pts.forEach(function (p) {
                const dot = svg('circle', {'class': 'chart-line-symbol ' + colour, cx: p[0], cy: p[1], r: 4});
                const tip = svg('title', {});
                tip.textContent = MONTHS[p[2]] + ' ' + s.year + ': ' + (Math.round(p[3] * 10) / 10) + ' hours';
                dot.appendChild(tip);
                group.appendChild(dot);
            });
            root.appendChild(group);
        });
        box.appendChild(root);

        if (!anyPoint) {
            box.appendChild(MD.el('div', {'class': 'chart-empty', text: 'No listening data for ' + data.selectedYear + ' yet'}));
        }
    }

    /**
     * Fills the colour-coded year title row above the chart (current v previous v highest).
     * @param {Object} data the chart payload
     */
    function drawTitle(data) {
        const row = byId('chartTitle');
        if (!row) return;
        row.innerHTML = '';
        const sep = function () { return MD.el('span', {'class': 'wt-smmd-bld', text: 'v'}); };
        row.appendChild(MD.el('span', {'class': 'year-header-current', text: String(data.selectedYear)}));
        if (data.hasPreviousYear) {
            row.appendChild(sep());
            row.appendChild(MD.el('span', {'class': 'year-header-previous', text: String(data.previousYear)}));
        }
        if (!data.isPreviousYearHighest && data.highestYear != null && data.highestYear !== data.selectedYear) {
            row.appendChild(sep());
            row.appendChild(MD.el('span', {'class': 'year-header-highest', text: String(data.highestYear)}));
        }
    }

    /**
     * Applies one delta (symbol + value) to its two spans: green up arrow, red down arrow, or a grey dash.
     * @param {string} symbolId id of the symbol span
     * @param {string} valueId id of the value span
     * @param {number|null} delta the delta in hours, or null for "no data"
     */
    function applyDelta(symbolId, valueId, delta) {
        const sym = byId(symbolId);
        const val = byId(valueId);
        if (!sym || !val) return;
        if (delta == null) {
            sym.textContent = '—';
            sym.className = 'graph-delta-main txt-grey-md';
            val.textContent = '';
            val.className = 'graph-delta-main txt-grey-md';
            return;
        }
        const cls = delta >= 0 ? 'lt-Green' : 'graph-delta-negative';
        sym.textContent = delta >= 0 ? '▲' : '▼';
        sym.className = 'graph-delta-main ' + cls;
        val.textContent = fmtInt(Math.abs(delta));
        val.className = 'graph-delta-main ' + cls;
    }

    /**
     * Updates the "All Time Stats" figures.
     * @param {Object} stats the stats part of the chart payload
     */
    function applyAllTime(stats) {
        byId('totalHours').textContent = fmtInt(stats.totalAllTimeHours);
        applyDelta('deltaPrevSymbol', 'deltaPrevValue', stats.hasPreviousYear ? stats.deltaVsPrevious : null);
        applyDelta('deltaAllSymbol', 'deltaAllValue', stats.deltaVsAllTimeHigh);
    }

    /**
     * Fetches the chart for a year and redraws the title, the chart and the all-time stats.
     * @param {number|string} year the selected year
     * @returns {Promise<void>} resolves when drawn
     */
    function loadChart(year) {
        return MD.api.get('/api/dashboard/chart?year=' + encodeURIComponent(year)).then(function (data) {
            lastChart = data;
            drawTitle(data);
            drawChart(data);
            applyAllTime(data.stats);
        }).catch(function (e) {
            MD.toast(e.message || 'Could not load the chart', 'error');
        });
    }

    /* ─── Stat cards ────────────────────────────────────────── */

    /**
     * Sets text, ids and hand cursor on one stat-card label from a StatLabel record.
     * @param {string} key the data-stat key (todaySong, todayAlbum, weeklySong, weeklyAlbum)
     * @param {{name:string, songId:?number, albumId:?number}} label the label data
     * @param {boolean} isSong whether it is a song label (tooltip and double-click play)
     */
    function applyStatLabel(key, label, isSong) {
        const el = document.querySelector('[data-stat="' + key + '"]');
        if (!el) return;
        el.textContent = label.name;
        delete el.dataset.songId;
        delete el.dataset.albumId;
        el.removeAttribute('title');
        el.classList.remove('Cursor-Hand');
        if (label.songId != null) el.dataset.songId = label.songId;
        if (label.albumId != null) el.dataset.albumId = label.albumId;
        const actionable = isSong ? label.songId != null : label.albumId != null;
        if (actionable && label.name !== '—') {
            el.classList.add('Cursor-Hand');
            el.title = isSong ? 'Click: view album  |  Double-click: play song' : 'Click to view album';
        }
    }

    /**
     * Writes a stat-card response into the cards.
     * @param {Object} s the /api/dashboard/stats response
     */
    function applyStats(s) {
        const set = function (key, text) {
            const el = document.querySelector('[data-stat="' + key + '"]');
            if (el) el.textContent = text;
        };
        set('playbackValue', s.playbackValue);
        set('playbackUnit', s.playbackUnit);
        set('avgValue', s.avgValue);
        set('avgUnit', s.avgUnit);
        applyStatLabel('todaySong', s.todaySong, true);
        applyStatLabel('todayAlbum', s.todayAlbum, false);
        applyStatLabel('weeklySong', s.weeklySong, true);
        applyStatLabel('weeklyAlbum', s.weeklyAlbum, false);
    }

    /**
     * Wires click handling on the stat cards by delegation: a song label opens its album on a single
     * click and plays on a double click (the single click waits to see whether a second one follows);
     * an album label opens the album.
     */
    function wireStatCards() {
        const cards = byId('statCards');
        if (!cards) return;
        let timer = null;
        cards.addEventListener('click', function (ev) {
            const el = ev.target.closest('[data-stat]');
            if (!el || !el.classList.contains('Cursor-Hand')) return;
            const songId = el.dataset.songId;
            const albumId = el.dataset.albumId;
            if (songId != null) {
                if (ev.detail >= 2) {
                    clearTimeout(timer);
                    MD.player.playSong(Number(songId));
                } else if (albumId != null) {
                    clearTimeout(timer);
                    timer = setTimeout(function () { openAlbum(albumId); }, SINGLE_CLICK_DELAY_MS);
                }
            } else if (albumId != null) {
                openAlbum(albumId);
            }
        });
    }

    /* ─── Recently played ───────────────────────────────────── */

    /**
     * Rebuilds the Recently Played rows.
     * @param {Array<{songId:?number,title:string,relativeTime:string}>} rows the /api/dashboard/recent response
     */
    function applyRecent(rows) {
        const list = byId('recentList');
        if (!list) return;
        list.innerHTML = '';
        rows.forEach(function (r) {
            list.appendChild(MD.el('div', {
                'class': 'play-history-row',
                title: MD.touch.coarse() ? 'Tap to play' : 'Double-click to play',
                dataset: r.songId != null ? {songId: r.songId} : {}
            }, [
                MD.el('span', {'class': 'play-history-song', text: r.title}),
                MD.el('span', {'class': 'wt-smmd-bld', text: r.relativeTime})
            ]));
        });
    }

    /**
     * Plays the song of the Recently Played row that contains the event target, if any.
     * @param {Event} ev a click or dblclick event bubbling from inside #recentList
     * @returns {void}
     */
    function playRecentRow(ev) {
        const row = ev.target.closest('.play-history-row');
        if (row && row.dataset.songId) MD.player.playFromLibrary(Number(row.dataset.songId));
    }

    /**
     * Wires Recently Played rows (delegated, so rebuilt rows keep working): double-click plays with a
     * mouse, a single tap plays on touch (the dblclick handler ignores taps so a song never starts twice).
     * @returns {void}
     */
    function wireRecent() {
        const list = byId('recentList');
        if (!list) return;
        list.addEventListener('click', function (ev) {
            if (MD.touch.isTap(ev)) playRecentRow(ev);
        });
        list.addEventListener('dblclick', function (ev) {
            if (MD.touch.isTap(ev)) return;
            playRecentRow(ev);
        });
    }

    /* ─── Top artists ───────────────────────────────────────── */

    /**
     * Rebuilds the Top 5 Artists rows ("—" placeholders are not clickable).
     * @param {Array<{artistId:?number,name:string,time:string}>} rows the /api/dashboard/top-artists response
     */
    function applyTopArtists(rows) {
        const box = byId('topArtistRows');
        if (!box) return;
        box.innerHTML = '';
        rows.forEach(function (r) {
            const name = MD.el('span', {
                'class': 'wt-smmd-bld artist-name' + (r.artistId != null ? ' Cursor-Hand' : ''),
                text: r.name,
                dataset: r.artistId != null ? {artistId: r.artistId} : {}
            });
            box.appendChild(MD.el('div', {'class': 'artist-row'}, [
                name,
                MD.el('span', {'class': 'artist-time', text: r.time})
            ]));
        });
    }

    /**
     * Wires clicks on artist names (delegated).
     */
    function wireTopArtists() {
        const box = byId('topArtistRows');
        if (!box) return;
        box.addEventListener('click', function (ev) {
            const el = ev.target.closest('[data-artist-id]');
            if (el) openArtist(el.dataset.artistId);
        });
    }

    /* ─── Live refresh ──────────────────────────────────────── */

    /**
     * Re-fetches the stat cards, recently played list and top artists. A failed request is ignored
     * (the panels simply keep their previous content).
     * @returns {Promise<void>} resolves when all three requests finished
     */
    function refreshPanels() {
        const quiet = function () {};
        return Promise.all([
            MD.api.get('/api/dashboard/stats').then(applyStats).catch(quiet),
            MD.api.get('/api/dashboard/recent').then(applyRecent).catch(quiet),
            MD.api.get('/api/dashboard/top-artists').then(applyTopArtists).catch(quiet)
        ]).then(function () {});
    }

    /**
     * Handles md:songchange: refresh shortly after the change, and again ~21 s later when the
     * play has been recorded by the server. Earlier pending refreshes are cancelled.
     * @param {CustomEvent} ev the event; detail is the new song or null
     */
    function onSongChange(ev) {
        pendingTimers.forEach(clearTimeout);
        pendingTimers = [];
        if (!ev.detail) return;
        pendingTimers.push(setTimeout(refreshPanels, REFRESH_DELAY_MS));
        pendingTimers.push(setTimeout(refreshPanels, RECORD_DELAY_MS));
    }

    /* ─── Start-up ──────────────────────────────────────────── */

    /**
     * Initialises the page: wires interactions, draws the chart and listens for song changes.
     */
    function init() {
        wireStatCards();
        wireRecent();
        wireTopArtists();
        const select = byId('yearSelect');
        if (select) {
            select.addEventListener('change', function () { loadChart(select.value); });
            loadChart(select.value);
        }
        let resizeTimer = null;
        window.addEventListener('resize', function () {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(function () { if (lastChart) drawChart(lastChart); }, 100);
        });
        document.addEventListener('md:songchange', onSongChange);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
