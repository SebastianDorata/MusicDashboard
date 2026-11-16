/*
 * playlist.js - behaviour of the "My Playlist" page (/playlist).
 *
 * Ports the desktop PlaylistViewBuilder / PlaylistDialogHandler interactions:
 *   - "New Playlist" dialog (create, then the new playlist is selected)
 *   - Rename and Delete dialogs for the selected playlist (rename reloads it, delete returns to the browser)
 *   - Play All (plays the first song), card click / row double-click plays a song;
 *     on touch a single tap on a row plays it too (MD.touch.isTap)
 *   - song menu ("Play", "Remove from Playlist"): right-click, or the touch-only "⋯" button on cards and rows
 * The page state (selected playlist, display mode) is the URL, so every change reloads the page.
 * Depends on window.MD (app.js) and MD.player (player.js).
 */
(function () {
    'use strict';

    const page = document.getElementById('playlist-page');
    if (!page) return;

    const playlistId = page.dataset.playlistId || null;
    const playlistName = page.dataset.playlistName || '';
    const playlistDesc = page.dataset.playlistDesc || '';
    const mode = page.dataset.mode || 'grid';

    /**
     * Builds the page URL for a playlist selection, keeping the display mode.
     * @param {string|number|null} id the playlist id, or null for the browser
     * @returns {string} the URL
     */
    function pageUrl(id) {
        return '/playlist?' + (id ? 'id=' + encodeURIComponent(id) + '&' : '') + 'mode=' + mode;
    }

    /**
     * Opens the name/description form used by create and rename (desktop showForm).
     * The OK button stays disabled while the name is empty.
     * @param {string} title dialog title
     * @param {string} header text shown at the top of the dialog
     * @param {string} initialName pre-populated name
     * @param {string} initialDesc pre-populated description
     * @param {function(string, string): void} onOk called with the trimmed name and description
     * @returns {void}
     */
    function showForm(title, header, initialName, initialDesc, onOk) {
        const nameInput = MD.el('input', {type: 'text', class: 'form-input', placeholder: 'Playlist name', value: initialName || ''});
        const descInput = MD.el('input', {type: 'text', class: 'form-input', placeholder: 'Description (optional)', value: initialDesc || ''});
        const content = MD.el('div', {}, [
            MD.el('div', {class: 'playlist-dialog-header', text: header}),
            MD.el('div', {class: 'form-grid'}, [
                MD.el('label', {text: 'Name:'}), nameInput,
                MD.el('label', {text: 'Description:'}), descInput
            ])
        ]);
        const dlg = MD.modal.open({
            title: title,
            content: content,
            buttons: [
                {label: 'Cancel', className: 'btn-blue', onClick: function (close) { close(); }},
                {label: 'OK', className: 'green-btn', onClick: function (close) {
                    const name = nameInput.value.trim();
                    if (!name) return;
                    close();
                    onOk(name, descInput.value.trim());
                }}
            ]
        });
        const ok = dlg.el.querySelector('.md-modal-footer button.green-btn');
        /** Enables OK only when the name is not blank. */
        function sync() { ok.disabled = nameInput.value.trim() === ''; }
        nameInput.addEventListener('input', sync);
        nameInput.addEventListener('keydown', function (e) { if (e.key === 'Enter' && !ok.disabled) ok.click(); });
        descInput.addEventListener('keydown', function (e) { if (e.key === 'Enter' && !ok.disabled) ok.click(); });
        sync();
        nameInput.focus();
    }

    /**
     * Shows the desktop's error dialog for a failed action.
     * @param {string} prefix the message prefix, e.g. "Could not create playlist: "
     * @param {Error} err the failure
     * @returns {void}
     */
    function showError(prefix, err) {
        MD.modal.alert(prefix + err.message);
    }

    /** Create dialog: creates the playlist and selects it. */
    function showCreateDialog() {
        showForm('New Playlist', 'Create a new playlist', '', '', function (name, description) {
            MD.api.post('/api/playlists', {name: name, description: description})
                .then(function (created) { window.location.href = pageUrl(created.id); })
                .catch(function (e) { showError('Could not create playlist: ', e); });
        });
    }

    /** Rename dialog: updates name and description, then reloads the selected playlist. */
    function showRenameDialog() {
        showForm('Rename Playlist', 'Rename "' + playlistName + '"', playlistName, playlistDesc, function (name, description) {
            MD.api.put('/api/playlists/' + playlistId, {name: name, description: description})
                .then(function () { window.location.href = pageUrl(playlistId); })
                .catch(function (e) { showError('Could not rename playlist: ', e); });
        });
    }

    /** Delete confirmation: deletes the playlist and returns to the playlist browser. */
    function showDeleteDialog() {
        const content = MD.el('div', {}, [
            MD.el('div', {class: 'playlist-dialog-header', text: 'Delete "' + playlistName + '"?'}),
            MD.el('div', {text: 'This permanently deletes the playlist. Songs remain in your library.'})
        ]);
        MD.modal.open({
            title: 'Delete Playlist',
            content: content,
            buttons: [
                {label: 'Cancel', className: 'btn-blue', onClick: function (close) { close(); }},
                {label: 'OK', className: 'green-btn', onClick: function (close) {
                    close();
                    MD.api.del('/api/playlists/' + playlistId)
                        .then(function () { window.location.href = pageUrl(null); })
                        .catch(function (e) { showError('Could not delete playlist: ', e); });
                }}
            ]
        });
    }

    /**
     * Removes a song from the selected playlist and reloads the playlist view.
     * @param {string} songId the song id
     * @returns {void}
     */
    function removeSong(songId) {
        MD.api.del('/api/playlists/' + playlistId + '/songs/' + songId)
            .then(function () { window.location.href = pageUrl(playlistId); })
            .catch(function (e) { showError('Could not remove song: ', e); });
    }

    /**
     * Plays one song (desktop musicPlayerService.playSong).
     * @param {string} songId the song id
     * @returns {void}
     */
    function play(songId) {
        MD.player.playSong(Number(songId));
    }

    /**
     * Builds the song menu items shared by right-click and the "⋯" button.
     * @param {string} songId the song id
     * @returns {Array<{label: string, onClick: function(): void}>} the menu items
     */
    function songMenuItems(songId) {
        return [
            {label: '▶  Play', onClick: function () { play(songId); }},
            {label: '✕  Remove from Playlist', onClick: function () { removeSong(songId); }}
        ];
    }

    /**
     * Opens the song context menu ("Play", "Remove from Playlist").
     * @param {MouseEvent} e the contextmenu event
     * @param {string} songId the song id
     * @returns {void}
     */
    function showSongMenu(e, songId) {
        e.preventDefault();
        MD.menu.show(e, songMenuItems(songId));
    }

    /**
     * Wires a "⋯" button to open the song menu anchored on the button. Stops propagation so a
     * card's own click handler does not also play the song.
     * @param {Element} owner the card or row containing the button
     * @param {string} songId the song id
     * @returns {void}
     */
    function wireMenuButton(owner, songId) {
        const btn = owner.querySelector('.song-row-menu-btn');
        if (!btn) return;
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            MD.menu.show(btn, songMenuItems(songId));
        });
    }

    /**
     * Wires up header buttons, Play All, and the song cards / rows (play on click / double-click / tap,
     * the right-click menu and the "⋯" menu button).
     * @returns {void}
     */
    function init() {
        const newBtn = document.getElementById('new-playlist-btn');
        if (newBtn) newBtn.addEventListener('click', showCreateDialog);
        if (!playlistId) return;

        const rename = document.getElementById('rename-btn');
        const del = document.getElementById('delete-btn');
        const playAll = document.getElementById('play-all-btn');
        if (rename) rename.addEventListener('click', showRenameDialog);
        if (del) del.addEventListener('click', showDeleteDialog);
        // Desktop playAll: plays the first song only, no queue
        if (playAll) playAll.addEventListener('click', function () {
            const first = page.querySelector('[data-song-id]');
            if (first) play(first.dataset.songId);
        });

        page.querySelectorAll('.playlist-song-card[data-song-id]').forEach(function (card) {
            card.addEventListener('click', function () { play(card.dataset.songId); });
            card.addEventListener('contextmenu', function (e) { showSongMenu(e, card.dataset.songId); });
            wireMenuButton(card, card.dataset.songId);
        });
        page.querySelectorAll('.playlist-song-row[data-song-id]').forEach(function (row) {
            row.addEventListener('dblclick', function (e) {
                if (MD.touch.isTap(e)) return;
                play(row.dataset.songId);
            });
            // Touch: a single tap plays, unless it landed on the "⋯" button
            row.addEventListener('click', function (e) {
                if (e.target.closest('.song-row-menu-btn')) return;
                if (MD.touch.isTap(e)) play(row.dataset.songId);
            });
            wireMenuButton(row, row.dataset.songId);
            row.addEventListener('contextmenu', function (e) { showSongMenu(e, row.dataset.songId); });
        });
    }

    init();
})();
