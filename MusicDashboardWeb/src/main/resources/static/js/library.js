/*
 * library.js - interactions of the My Library page (desktop MyLibraryController and the
 * presentation/libraryViews classes).
 *
 * The page itself is rendered by the server (view state = query string). This script adds:
 *   - genre / sort controls that submit their form on change (LibraryTopBarBuilder)
 *   - the alphabet bar that scrolls to a section divider (AlphabetBar)
 *   - playback: double-click a song row -> MD.player.playQueue(sortedIds, id) (queue rule of
 *     SongViewBuilder); click an album-detail row -> playQueue(albumSongs, id) (AlbumViewBuilder);
 *     click a favourites grid card -> MD.player.playSong(id) (CardFactory.createSongCard)
 *   - the "⋯" song menu and the Edit Song dialog (SongHandler, SongEditDialog)
 *   - the album right-click menu, Edit Album dialog and Delete Album confirmation
 *     (AlbumViewHelper, AlbumEditDialog)
 *
 * Depends on window.MD (app.js) and MD.player (player.js).
 */
(function () {
    'use strict';

    /* ─── Small helpers ─────────────────────────────────────── */

    /**
     * Reads the numeric id stored in a data attribute.
     * @param {HTMLElement} el the element carrying the attribute
     * @param {string} name the dataset key
     * @returns {number} the id
     */
    function idOf(el, name) {
        return Number(el.dataset[name]);
    }

    /**
     * Shows an error dialog (desktop AppUtils.showError).
     * @param {string} message the text to show
     * @returns {Promise<void>} resolves when closed
     */
    function showError(message) {
        return MD.modal.alert(message, 'Error');
    }

    /**
     * Opens a dialog with an optional bold header line above its body (desktop Dialog headerText).
     * @param {{title:string, header:(string|null), body:(Node|null), buttons:Array, onClose:function}} opts dialog options
     * @returns {{el: HTMLElement, close: function(): void}} the dialog handle
     */
    function openDialog(opts) {
        const content = MD.el('div', {class: 'library-dialog'});
        if (opts.header) {
            content.appendChild(MD.el('div', {
                text: opts.header,
                style: {fontWeight: 'bold', marginBottom: '12px', whiteSpace: 'pre-wrap'}
            }));
        }
        if (opts.body) content.appendChild(opts.body);
        return MD.modal.open({title: opts.title, content: content, buttons: opts.buttons, onClose: opts.onClose});
    }

    /**
     * Shows an informational dialog with a single OK button (desktop Alert INFORMATION).
     * @param {string} title the dialog title
     * @param {string} message the text to show
     * @returns {Promise<void>} resolves when closed
     */
    function info(title, message) {
        return MD.modal.alert(message, title);
    }

    /**
     * Shows an OK / Cancel confirmation with a header and content text (desktop Alert CONFIRMATION).
     * @param {{title:string, header:string, content:string, okLabel:string, cancelLabel:string, danger:boolean}} opts texts
     * @returns {Promise<boolean>} true when confirmed
     */
    function confirmDialog(opts) {
        return new Promise(function (resolve) {
            let answer = false;
            openDialog({
                title: opts.title,
                header: opts.header,
                body: MD.el('div', {text: opts.content, style: {whiteSpace: 'pre-wrap'}}),
                buttons: [
                    {label: opts.okLabel || 'OK', className: opts.danger ? 'btn-red' : 'green-btn',
                        onClick: function (close) { answer = true; close(); }},
                    {label: opts.cancelLabel || 'Cancel', className: 'btn-blue', onClick: function (close) { close(); }}
                ],
                onClose: function () { resolve(answer); }
            });
        });
    }

    /**
     * Builds one labelled text field row for the edit dialogs.
     * @param {string} label the label text
     * @param {string} value the initial value
     * @param {string} [prompt] the placeholder text
     * @returns {{label: HTMLElement, input: HTMLInputElement}} the label and input elements
     */
    function field(label, value, prompt) {
        return {
            label: MD.el('label', {text: label, style: {textAlign: 'right'}}),
            input: MD.el('input', {type: 'text', class: 'form-input dialog-text-field', value: value || '',
                placeholder: prompt || ''})
        };
    }

    /* ─── Filters & alphabet bar ────────────────────────────── */

    /**
     * Submits the genre / sort form whenever either dropdown changes (desktop onGenreFilter / onSortChange).
     * @returns {void}
     */
    function initFilters() {
        const form = document.querySelector('form[data-role="filters"]');
        if (!form) return;
        form.addEventListener('change', function () { form.submit(); });
    }

    /**
     * Wires the alphabet bar: clicking a letter scrolls its divider to the top of the content area.
     * @returns {void}
     */
    function initAlphabetBar() {
        document.addEventListener('click', function (e) {
            const btn = e.target.closest('.alphabet-letter-active');
            if (!btn) return;
            const layout = btn.closest('.library-list-layout');
            const letter = btn.dataset.letter;
            const target = layout && Array.prototype.find.call(layout.querySelectorAll('[data-letter]'),
                function (n) { return n.dataset.letter === letter && !n.classList.contains('alphabet-letter-active'); });
            if (target) target.scrollIntoView({block: 'start'});
        });
    }

    /* ─── Playback ──────────────────────────────────────────── */

    /**
     * Returns the song ids a click should queue: the container's data-queue (sorted order) when
     * present, otherwise every song row of the same list in document order.
     * @param {HTMLElement} row the clicked song row
     * @returns {number[]} the ids
     */
    function queueFor(row) {
        const holder = row.closest('[data-queue]');
        if (holder && holder.dataset.queue) {
            return holder.dataset.queue.split(',').map(Number).filter(function (n) { return n > 0; });
        }
        return Array.prototype.map.call(row.parentElement.querySelectorAll('[data-song-id]'),
            function (r) { return Number(r.dataset.songId); });
    }

    /**
     * Wires playback: double-click on list rows, click on album-detail rows and favourites cards.
     * @returns {void}
     */
    function initPlayback() {
        document.addEventListener('dblclick', function (e) {
            const row = e.target.closest('.library-song-row');
            if (!row || e.target.closest('.song-row-menu-btn')) return;
            const id = idOf(row, 'songId');
            MD.player.playQueue(queueFor(row), id);
        });
        document.addEventListener('click', function (e) {
            if (e.target.closest('.song-row-menu-btn')) return;
            const detailRow = e.target.closest('.album-song-row');
            if (detailRow) {
                MD.player.playQueue(queueFor(detailRow), idOf(detailRow, 'songId'));
                return;
            }
            const card = e.target.closest('[data-play="single"]');
            if (card) MD.player.playSong(idOf(card, 'songId'));
        });
        document.addEventListener('keydown', function (e) {
            if (e.key !== 'Enter') return;
            const card = e.target.closest && e.target.closest('[data-play="single"]');
            if (card) MD.player.playSong(idOf(card, 'songId'));
        });
    }

    /* ─── Song menu ─────────────────────────────────────────── */

    /**
     * Opens the song menu below the "⋯" button with the SongHandler items.
     * The favourite state is read from the server at open time, like the desktop did.
     * @param {HTMLElement} btn the "⋯" button
     * @returns {Promise<void>} resolves when the menu is shown
     */
    async function openSongMenu(btn) {
        const row = btn.closest('[data-song-id]');
        const id = idOf(row, 'songId');
        const title = row.dataset.title;
        let isFav = false;
        try {
            isFav = !!(await MD.api.get('/api/library/songs/' + id)).favourited;
        } catch (err) {
            isFav = false;
        }
        MD.menu.show(btn, [
            {label: '＋  Add to Playlist', onClick: function () { addToPlaylist(id, title); }},
            {label: isFav ? '♥  Remove from Favourites' : '♡  Add to Favourites',
                onClick: function () { toggleFavourite(id); }},
            {label: '✎  Edit Song', onClick: function () { editSong(id); }}
        ]);
    }

    /**
     * Toggles the favourite flag of a song.
     * @param {number} id the song id
     * @returns {Promise<void>} resolves when done
     */
    async function toggleFavourite(id) {
        try {
            await MD.api.post('/api/library/songs/' + id + '/favourite');
        } catch (err) {
            await showError('Could not update favourites: ' + err.message);
        }
    }

    /**
     * Runs the Add to Playlist flow: playlist picker, then add and report the result.
     * @param {number} songId the song id
     * @param {string} title the song title (for the dialog texts)
     * @returns {Promise<void>} resolves when the flow ends
     */
    async function addToPlaylist(songId, title) {
        let playlists;
        try {
            playlists = (await MD.api.get('/api/playlists')) || [];
        } catch (err) {
            await showError('Could not load playlists: ' + err.message);
            return;
        }
        if (!playlists.length) {
            await info('No Playlists', 'You have no playlists yet. Create one from the Playlists screen.');
            return;
        }

        let selected = null;
        const list = MD.el('div', {class: 'library-picker', role: 'listbox',
            style: {maxHeight: '200px', overflowY: 'auto', border: '1px solid #404040', borderRadius: '6px'}});
        const dlg = openDialog({
            title: 'Add to Playlist',
            header: 'Choose a playlist for "' + title + '"',
            body: list,
            buttons: [
                {label: 'OK', className: 'green-btn', onClick: function (close) {
                    if (!selected) return;
                    const pick = selected;
                    close();
                    confirmAdd(songId, title, pick);
                }},
                {label: 'Cancel', className: 'btn-blue', onClick: function (close) { close(); }}
            ]
        });
        const ok = dlg.el.querySelector('.md-modal-footer button');
        ok.disabled = true;
        playlists.forEach(function (pl) {
            const item = MD.el('div', {
                role: 'option', tabindex: '0',
                text: pl.name + '  (' + pl.songCount + ' songs)',
                style: {padding: '8px 12px', cursor: 'pointer', whiteSpace: 'pre'}
            });
            /** Marks this playlist as the picked one and enables OK. */
            const pick = function () {
                selected = pl;
                Array.prototype.forEach.call(list.children, function (c) {
                    c.style.background = '';
                    c.setAttribute('aria-selected', 'false');
                });
                item.style.background = 'rgba(29, 185, 84, 0.25)';
                item.setAttribute('aria-selected', 'true');
                ok.disabled = false;
            };
            item.addEventListener('click', pick);
            item.addEventListener('keydown', function (e) {
                if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); pick(); }
            });
            list.appendChild(item);
        });
    }

    /**
     * Adds the song to the chosen playlist and shows the "Added" / "Already Added" message.
     * @param {number} songId the song id
     * @param {string} title the song title
     * @param {{id:number, name:string}} playlist the chosen playlist
     * @returns {Promise<void>} resolves when the message is closed
     */
    async function confirmAdd(songId, title, playlist) {
        try {
            const res = await MD.api.post('/api/playlists/' + playlist.id + '/songs/' + songId);
            if (res && res.added === false) {
                await info('Already Added', 'That song is already in "' + playlist.name + '".');
            } else {
                await info('Added', '"' + title + '" added to "' + playlist.name + '".');
            }
        } catch (err) {
            await showError('Could not add song to playlist: ' + err.message);
        }
    }

    /* ─── Edit Song dialog ──────────────────────────────────── */

    /**
     * Loads the song and opens the Edit Song dialog.
     * @param {number} id the song id
     * @returns {Promise<void>} resolves when the dialog is open
     */
    async function editSong(id) {
        let details;
        try {
            details = await MD.api.get('/api/library/songs/' + id);
        } catch (err) {
            await showError(err.message);
            return;
        }
        showEditSong(details, null);
    }

    /**
     * Builds the summary of changes shown on the confirmation step (SongEditDialog texts).
     * @param {Object} old the song details before editing
     * @param {Object} now the trimmed entered values {title, album, artists, genres, track}
     * @returns {string} the summary, one line per change, empty when nothing changed
     */
    function describeChanges(old, now) {
        let out = '';
        const line = function (label, before, after) {
            if (after && after !== before) out += label + before + '  →  ' + after + '\n';
        };
        line('Title:   ', old.title || '', now.title);
        line('Album:   ', old.album || '', now.album);
        line('Artists: ', old.artists || '', now.artists);
        line('Genres:  ', old.genres || '', now.genres);
        line('Track #: ', old.trackNumber || '', now.track);
        return out;
    }

    /**
     * Opens the Edit Song dialog; Save leads to the "Confirm Changes" step, "Go Back" returns here.
     * @param {Object} details the current song details from the server
     * @param {Object|null} entered previously entered values to restore (after "Go Back")
     * @returns {void}
     */
    function showEditSong(details, entered) {
        const start = entered || {title: details.title, album: details.album, artists: details.artists,
            genres: details.genres, track: details.trackNumber};
        const t = field('Title', start.title);
        const a = field('Album', start.album);
        const ar = field('Artist(s)', start.artists);
        const g = field('Genre(s)', start.genres);
        const tr = field('Track #', start.track);
        const grid = MD.el('div', {class: 'form-grid'}, [
            t.label, t.input, a.label, a.input, ar.label, ar.input, g.label, g.input, tr.label, tr.input,
            MD.el('span'),
            MD.el('span', {text: 'Separate multiple artists/genres with a comma',
                style: {fontSize: '10px', color: 'gray'}})
        ]);

        /**
         * Reads the form, and either closes (nothing changed) or opens the confirmation step.
         * @param {function(): void} close closes the edit dialog
         */
        const save = function (close) {
            const now = {title: t.input.value.trim(), album: a.input.value.trim(),
                artists: ar.input.value.trim(), genres: g.input.value.trim(), track: tr.input.value.trim()};
            const changes = describeChanges(details, now);
            close();
            if (!changes) return;
            confirmChanges(details, now, changes);
        };

        const dlg = openDialog({
            title: 'Edit Song',
            header: 'Editing: ' + details.title,
            body: grid,
            buttons: [
                {label: 'Save', className: 'green-btn', onClick: save},
                {label: 'Cancel', className: 'btn-blue', onClick: function (close) { close(); }}
            ]
        });
        dlg.el.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' && e.target.tagName === 'INPUT') {
                e.preventDefault();
                save(dlg.close);
            }
        });
    }

    /**
     * Shows the "Confirm Changes" dialog; Confirm saves, Go Back reopens the edit dialog.
     * @param {Object} details the song details before editing
     * @param {Object} now the entered values
     * @param {string} changes the change summary text
     * @returns {void}
     */
    function confirmChanges(details, now, changes) {
        openDialog({
            title: 'Confirm Changes',
            header: 'Save the following changes?',
            body: MD.el('div', {text: changes, style: {whiteSpace: 'pre-wrap', fontFamily: 'monospace'}}),
            buttons: [
                {label: 'Confirm', className: 'green-btn', onClick: async function (close) {
                    close();
                    try {
                        await MD.api.put('/api/library/songs/' + details.id, {
                            title: now.title, album: now.album, artists: now.artists,
                            genres: now.genres, trackNumber: now.track
                        });
                        window.location.reload();
                    } catch (err) {
                        await showError(err.message);
                    }
                }},
                {label: 'Go Back', className: 'btn-blue', onClick: function (close) {
                    close();
                    showEditSong(details, now);
                }}
            ]
        });
    }

    /**
     * Wires the "⋯" buttons and the right-click shortcut that opens Edit Song on Songs-tab rows.
     * @returns {void}
     */
    function initSongMenu() {
        document.addEventListener('click', function (e) {
            const btn = e.target.closest('[data-action="song-menu"]');
            if (!btn) return;
            e.preventDefault();
            e.stopPropagation();
            openSongMenu(btn);
        }, true);
        document.addEventListener('contextmenu', function (e) {
            const row = e.target.closest('.library-song-row');
            if (!row) return;
            e.preventDefault();
            if (row.dataset.editable === 'true') editSong(idOf(row, 'songId'));
        });
    }

    /* ─── Albums: context menu, edit, delete ────────────────── */

    /**
     * Wires the right-click menu of album cards and rows (Edit Album, Delete Album).
     * Inside the artist detail grid the menu is suppressed, like the desktop did.
     * @returns {void}
     */
    function initAlbumMenu() {
        document.addEventListener('contextmenu', function (e) {
            const host = e.target.closest('[data-album-id]');
            if (!host) return;
            const scope = host.closest('[data-album-menu]');
            e.preventDefault();
            if (!scope || scope.dataset.albumMenu !== 'true') return;
            MD.menu.show(e, [
                {label: '✏  Edit Album', onClick: function () { editAlbum(host); }},
                {label: '🗑  Delete Album', onClick: function () { deleteAlbum(host); }}
            ]);
        });
    }

    /**
     * Opens the Edit Album dialog pre-filled from the card / row data attributes.
     * @param {HTMLElement} host the album card or row
     * @returns {void}
     */
    function editAlbum(host) {
        const id = idOf(host, 'albumId');
        const t = field('Title', host.dataset.title, 'Album title');
        const y = field('Year', host.dataset.year, 'e.g. 2024');
        const g = field('Genre', host.dataset.genre, 'e.g. Hip-Hop');
        y.input.maxLength = 4;
        y.input.inputMode = 'numeric';
        y.input.addEventListener('input', function () {
            y.input.value = y.input.value.replace(/\D/g, '').slice(0, 4);
        });
        const note = MD.el('span', {class: 'txt-grey-md', text: 'Applied to all songs in this album',
            style: {fontSize: '10px'}});
        const grid = MD.el('div', {class: 'form-grid'}, [
            t.label, t.input, y.label, y.input, g.label, g.input, MD.el('span'), note
        ]);

        /**
         * Sends the edit and reloads the page.
         * @param {function(): void} close closes the dialog
         */
        const save = async function (close) {
            const title = t.input.value.trim();
            if (!title) return;
            const yearText = y.input.value.trim();
            const genre = g.input.value.trim();
            close();
            try {
                await MD.api.put('/api/library/albums/' + id, {
                    title: title,
                    releaseYear: yearText ? parseInt(yearText, 10) : null,
                    genre: genre || null
                });
                window.location.reload();
            } catch (err) {
                await showError(err.message);
            }
        };

        const dlg = openDialog({
            title: 'Edit Album',
            header: null,
            body: grid,
            buttons: [
                {label: 'Save', className: 'green-btn', onClick: save},
                {label: 'Cancel', className: 'btn-blue', onClick: function (close) { close(); }}
            ]
        });
        const saveBtn = dlg.el.querySelector('.md-modal-footer button');
        /** Disables Save while the title is blank. */
        const sync = function () { saveBtn.disabled = t.input.value.trim() === ''; };
        t.input.addEventListener('input', sync);
        sync();
        dlg.el.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' && e.target.tagName === 'INPUT' && !saveBtn.disabled) {
                e.preventDefault();
                save(dlg.close);
            }
        });
    }

    /**
     * Confirms and deletes an album with all its songs.
     * @param {HTMLElement} host the album card or row
     * @returns {Promise<void>} resolves when finished
     */
    async function deleteAlbum(host) {
        const id = idOf(host, 'albumId');
        const count = Number(host.dataset.songs) || 0;
        const ok = await confirmDialog({
            title: 'Delete Album',
            header: 'Delete "' + host.dataset.title + '"?',
            content: 'This permanently deletes the album and all ' + count + ' song(s) it contains, '
                + 'including their playback history, favourites, and playlist entries. This cannot be undone.'
        });
        if (!ok) return;
        try {
            await MD.api.del('/api/library/albums/' + id);
            window.location.reload();
        } catch (err) {
            await showError('Could not delete album: ' + err.message);
        }
    }

    /* ─── Start ─────────────────────────────────────────────── */
    initFilters();
    initAlphabetBar();
    initPlayback();
    initSongMenu();
    initAlbumMenu();
})();
