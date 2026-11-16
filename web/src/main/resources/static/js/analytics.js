/*
 * analytics.js - behaviour of the My Reports page (/analytics).
 *
 * Replaces the desktop AnalyticsController / AnalyticsPaginatedModal / ModalListController /
 * WeeklyReportCalendarController / YearWrappedViewController interactions:
 *   - the sidebar year selector submits its GET form when changed
 *   - clickable rows: a song row plays the song, an album row opens it in My Library
 *   - the "View All" buttons open a wide MD.modal that pages through /api/analytics/... in
 *     the desktop's page sizes (history 25, songs 10, albums 10, artists 20)
 *   - weekly calendar: hovering a day highlights the whole week
 *   - yearly wrapped: arrows / dots / keyboard switch between the ten slides
 * The sections themselves are rendered on the server (templates/analytics/*.html).
 */
(function () {
    'use strict';

    /** Modal definitions keyed by the button's data-modal value. */
    const MODALS = {
        history: {url: '/api/analytics/history', pageSize: 25, row: historyRow},
        songs:   {url: '/api/analytics/songs',   pageSize: 10, row: songRow},
        albums:  {url: '/api/analytics/albums',  pageSize: 10, row: albumRow},
        artists: {url: '/api/analytics/artists', pageSize: 20, row: artistRow}
    };

    /* ─── Row builders (AnalyticsRowFactory) ────────────────── */

    /**
     * Returns the text to show for a possibly missing value.
     * @param {string|null|undefined} v the value
     * @returns {string} the value, or an em dash when null/undefined
     */
    function txt(v) { return v == null ? '—' : String(v); }

    /**
     * Builds a listening-history row: title, artist, relative time; click plays the song.
     * @param {{songId:number|null, songTitle:string, artistName:string, playedAt:string}} r the row data
     * @returns {HTMLElement} the row
     */
    function historyRow(r) {
        return MD.el('div', {class: 'analytics-list-item clickable', dataset: r.songId != null ? {songId: r.songId} : {}}, [
            MD.el('span', {class: 'txt-white-sm row-primary', text: txt(r.songTitle)}),
            MD.el('span', {class: 'analytics-row-secondary row-secondary', text: txt(r.artistName)}),
            MD.el('span', {class: 'analytics-row-meta', text: txt(r.playedAt)})
        ]);
    }

    /**
     * Builds a top-song row: title and artist; click plays the song.
     * @param {{songId:number|null, songTitle:string, artistName:string}} r the row data
     * @returns {HTMLElement} the row
     */
    function songRow(r) {
        return MD.el('div', {class: 'analytics-list-item clickable', dataset: r.songId != null ? {songId: r.songId} : {}}, [
            MD.el('span', {class: 'txt-white-sm row-primary', text: txt(r.songTitle)}),
            MD.el('span', {class: 'analytics-row-secondary', text: txt(r.artistName)})
        ]);
    }

    /**
     * Builds a top-album row: title and "(year)"; click opens the album in My Library.
     * @param {{albumId:number|null, albumTitle:string, releaseYear:string}} r the row data
     * @returns {HTMLElement} the row
     */
    function albumRow(r) {
        return MD.el('div', {class: 'analytics-list-item clickable', dataset: r.albumId != null ? {albumId: r.albumId} : {}}, [
            MD.el('span', {class: 'txt-white-sm row-primary', text: txt(r.albumTitle)}),
            MD.el('span', {class: 'analytics-row-meta', text: '(' + r.releaseYear + ')'})
        ]);
    }

    /**
     * Builds a top-artist row: name and listening time (not clickable, as on the desktop).
     * @param {{artistName:string, listeningTime:string}} r the row data
     * @returns {HTMLElement} the row
     */
    function artistRow(r) {
        return MD.el('div', {class: 'analytics-list-item'}, [
            MD.el('span', {class: 'txt-white-sm row-primary', text: txt(r.artistName)}),
            MD.el('span', {class: 'analytics-row-secondary', text: txt(r.listeningTime)})
        ]);
    }

    /* ─── Paginated modal (ModalListController) ─────────────── */

    /**
     * Opens the paginated "view all" modal for one list and loads its first page.
     * Footer: First, Previous, "Page n / m", Next, Last. The header has a "← Go Back" button.
     * @param {string} key the MODALS key (history | songs | albums | artists)
     * @param {string} title the modal heading
     * @returns {void}
     */
    function openModal(key, title) {
        const cfg = MODALS[key];
        if (!cfg) return;
        let offset = 0;
        let total = 0;
        let seq = 0;

        const list = MD.el('div', {class: 'analytics-modal-list'});
        const pageLabel = MD.el('span', {class: 'wt-smmd-bld page-label', text: 'Page 1 / 1'});
        let handle = null;

        /** @returns {number} the number of pages (at least 1) */
        function totalPages() { return Math.max(1, Math.ceil(total / cfg.pageSize)); }

        /** Updates the "Page n / m" label from the current offset and total. */
        function updateLabel() {
            pageLabel.textContent = 'Page ' + (Math.floor(offset / cfg.pageSize) + 1) + ' / ' + totalPages();
        }

        /**
         * Fetches and renders the page starting at the current offset.
         * @returns {Promise<void>} resolves when the rows are shown
         */
        function load() {
            const mine = ++seq;
            return MD.api.get(cfg.url + '?offset=' + offset + '&limit=' + cfg.pageSize).then(function (page) {
                if (mine !== seq) return;
                total = page.total;
                list.replaceChildren.apply(list, page.items.map(cfg.row));
                list.scrollTop = 0;
                updateLabel();
            }).catch(function (e) { MD.toast(e.message, 'error'); });
        }

        /**
         * Moves to a new offset (clamped to the valid pages) and reloads.
         * @param {number} next the requested offset
         */
        function go(next) {
            const last = (totalPages() - 1) * cfg.pageSize;
            const clamped = Math.max(0, Math.min(next, last));
            if (clamped === offset) return;
            offset = clamped;
            updateLabel();
            load();
        }

        /**
         * Builds one pagination button.
         * @param {string} label the button text
         * @param {function(): void} fn the click handler
         * @returns {HTMLButtonElement} the button
         */
        function btn(label, fn) {
            return MD.el('button', {type: 'button', class: 'btn-blue', text: label, on: {click: fn}});
        }

        const content = MD.el('div', {class: 'analytics-modal'}, [
            MD.el('div', {class: 'analytics-modal-header'}, [
                MD.el('span', {class: 'wt-smmd-bld', text: title}),
                MD.el('button', {type: 'button', class: 'return-btn', text: '← Go Back',
                    on: {click: function () { if (handle) handle.close(); }}})
            ]),
            list,
            MD.el('div', {class: 'analytics-modal-footer'}, [
                btn('⟨⟨ First', function () { go(0); }),
                btn('⟨ Previous', function () { go(offset - cfg.pageSize); }),
                pageLabel,
                btn('Next ⟩', function () { go(offset + cfg.pageSize); }),
                btn('Last ⟩⟩', function () { go((totalPages() - 1) * cfg.pageSize); })
            ])
        ]);

        handle = MD.modal.open({wide: true, content: content});
        load();
    }

    /* ─── Page wiring ───────────────────────────────────────── */

    /**
     * Handles a click on a clickable row: plays the song or opens the album in My Library.
     * @param {MouseEvent} e the click event (delegated from the document)
     * @returns {void}
     */
    function onRowClick(e) {
        const row = e.target.closest('.analytics-list-item.clickable');
        if (!row) return;
        if (row.dataset.songId) {
            if (window.MD && MD.player) MD.player.playSong(Number(row.dataset.songId));
        } else if (row.dataset.albumId) {
            window.location.href = '/library?album=' + encodeURIComponent(row.dataset.albumId);
        }
    }

    /**
     * Highlights every day of the hovered calendar week.
     * @param {HTMLElement} grid the calendar grid element
     * @returns {void}
     */
    function wireCalendar(grid) {
        /**
         * Adds or removes the highlight class on all days of one week.
         * @param {string|null} week the week number, or null to clear
         */
        function mark(week) {
            grid.querySelectorAll('.calendar-day').forEach(function (d) {
                d.classList.toggle('week-hover', week != null && d.dataset.week === week);
            });
        }
        grid.addEventListener('mouseover', function (e) {
            const d = e.target.closest('.calendar-day');
            if (d) mark(d.dataset.week);
        });
        grid.addEventListener('mouseleave', function () { mark(null); });
    }

    /**
     * Wires the wrapped slideshow: arrow buttons, dot indicators and arrow keys.
     * @param {HTMLElement} root the #wrapped element
     * @returns {void}
     */
    function wireWrapped(root) {
        const slides = Array.from(root.querySelectorAll('[data-slide]'));
        const dots = Array.from(root.querySelectorAll('[data-dot]'));
        let current = 0;

        /**
         * Shows one slide and updates the dots.
         * @param {number} idx the slide index
         */
        function goTo(idx) {
            if (idx < 0 || idx >= slides.length) return;
            current = idx;
            slides.forEach(function (s, i) { s.classList.toggle('active', i === idx); });
            dots.forEach(function (d, i) {
                d.classList.toggle('nav-dot-active', i === idx);
                d.classList.toggle('nav-dot', i !== idx);
            });
        }
        root.querySelector('[data-wrapped="prev"]').addEventListener('click', function () { goTo(current - 1); });
        root.querySelector('[data-wrapped="next"]').addEventListener('click', function () { goTo(current + 1); });
        dots.forEach(function (d, i) { d.addEventListener('click', function () { goTo(i); }); });
        document.addEventListener('keydown', function (e) {
            if (document.querySelector('.md-modal-backdrop')) return;
            if (e.key === 'ArrowLeft') goTo(current - 1);
            else if (e.key === 'ArrowRight') goTo(current + 1);
        });
    }

    /**
     * Initialises the page once the DOM is ready.
     * @returns {void}
     */
    function init() {
        const year = document.getElementById('yearSelect');
        if (year) year.addEventListener('change', function () { document.getElementById('yearForm').submit(); });

        document.addEventListener('click', onRowClick);
        document.querySelectorAll('[data-modal]').forEach(function (b) {
            b.addEventListener('click', function () { openModal(b.dataset.modal, b.dataset.title); });
        });

        const grid = document.querySelector('.calendar-grid');
        if (grid) wireCalendar(grid);
        const wrapped = document.getElementById('wrapped');
        if (wrapped) wireWrapped(wrapped);
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();
})();
