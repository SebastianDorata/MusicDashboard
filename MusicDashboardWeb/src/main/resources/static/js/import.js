/*
 * import.js - behaviour of the Import Music page (/import).
 *
 * Two entry points feed the same server-side import pipeline:
 *   1. Start Scan: POST /api/import/scan {path} imports a folder on the machine running the app.
 *   2. Drop zone / Browse: dropped or picked files and folders are filtered to .mp3/.m4a and
 *      uploaded with MD.api.upload to /api/import/upload (multipart fields "files" and "paths").
 * Both return {jobId}; the page then polls GET /api/import/jobs/{jobId} about twice a second to
 * drive the progress bar, the progress label, the summary line and the results table, using the
 * desktop screen's texts, status labels and colours.
 */
(function () {
    'use strict';

    const POLL_MS = 500;
    const NO_FILES_TEXT = 'No supported audio files found (.mp3, .m4a)';

    /** Status label and colour per MigrationResult.Status, exactly as on the desktop. */
    const STATUS = {
        IMPORTED:         { label: '＋  Imported',         colour: '#1db954' },
        PATH_UPDATED:     { label: '✔  Path Updated',      colour: '#1db954' },
        METADATA_UPDATED: { label: '✎  Metadata Updated',  colour: '#f7931e' },
        ALREADY_CURRENT:  { label: '·  Already Current',   colour: '#999999' },
        ART_REFRESHED:    { label: '⊙  Art Refreshed',     colour: '#61dafb' },
        SKIPPED:          { label: '—  Skipped',           colour: '#777777' },
        ERROR:            { label: '✕  Error',             colour: '#e94560' }
    };

    const folderPath = document.getElementById('folderPath');
    const scanBtn = document.getElementById('scanBtn');
    const dropZone = document.getElementById('dropZone');
    const browseBtn = document.getElementById('browseBtn');
    const fileInput = document.getElementById('fileInput');
    const folderInput = document.getElementById('folderInput');
    const track = document.getElementById('progressTrack');
    const fill = document.getElementById('progressFill');
    const progressLabel = document.getElementById('progressLabel');
    const summaryLabel = document.getElementById('summaryLabel');
    const resultsBody = document.getElementById('resultsBody');

    let pollTimer = null;
    let busy = false;

    /* ─── Display helpers ───────────────────────────────────── */

    /**
     * Sets the progress bar fill.
     * @param {number} fraction value between 0 and 1
     */
    function setProgress(fraction) {
        fill.style.width = Math.max(0, Math.min(1, fraction)) * 100 + '%';
    }

    /** Clears the progress bar, labels and results table (desktop resetProgress). */
    function resetProgress() {
        if (pollTimer) { clearTimeout(pollTimer); pollTimer = null; }
        setProgress(0);
        track.classList.add('hidden');
        progressLabel.textContent = '';
        summaryLabel.textContent = '';
        renderResults([]);
    }

    /**
     * Enables or disables the Start Scan button from the busy flag and the path field.
     */
    function updateScanButton() {
        scanBtn.disabled = busy || folderPath.value.trim() === '';
    }

    /**
     * Counts the results with a given status.
     * @param {Array} results result rows
     * @param {string} status status constant name
     * @returns {number} how many rows have that status
     */
    function count(results, status) {
        return results.filter(function (r) { return r.status === status; }).length;
    }

    /**
     * Renders the results table body, or the empty placeholder.
     * @param {Array<{fileName:string,status:string,message:string}>} results rows to show
     */
    function renderResults(results) {
        resultsBody.textContent = '';
        if (!results.length) {
            resultsBody.appendChild(MD.el('tr', { class: 'import-placeholder' },
                MD.el('td', { colspan: 3, text: 'Results will appear here after importing.' })));
            return;
        }
        const frag = document.createDocumentFragment();
        results.forEach(function (r) {
            const st = STATUS[r.status] || { label: r.status, colour: '#ffffff' };
            frag.appendChild(MD.el('tr', {}, [
                MD.el('td', { text: r.fileName }),
                MD.el('td', { class: 'import-status', text: st.label, style: { color: st.colour } }),
                MD.el('td', { text: r.message || '' })
            ]));
        });
        resultsBody.appendChild(frag);
    }

    /**
     * Shows the finished state: summary line, final label and results table.
     * @param {Array} results the job's result rows
     */
    function showDone(results) {
        setProgress(1);
        summaryLabel.textContent =
            'Done.  ' + count(results, 'IMPORTED') + ' new  ·  ' +
            count(results, 'PATH_UPDATED') + ' relocated  ·  ' +
            count(results, 'METADATA_UPDATED') + ' metadata refreshed  ·  ' +
            count(results, 'ART_REFRESHED') + ' art refreshed  ·  ' +
            count(results, 'ALREADY_CURRENT') + ' already current  ·  ' +
            count(results, 'SKIPPED') + ' skipped  ·  ' +
            count(results, 'ERROR') + ' error(s)';
        progressLabel.textContent = 'All done — ' + results.length + ' file(s) evaluated.';
        renderResults(results);
    }

    /** Marks the page idle again after a job ends or fails to start. */
    function finish() {
        busy = false;
        updateScanButton();
    }

    /* ─── Job polling ───────────────────────────────────────── */

    /**
     * Polls a job until it ends, updating the bar and label on every tick.
     * @param {string} jobId the id returned by the scan/upload endpoint
     */
    function poll(jobId) {
        MD.api.get('/api/import/jobs/' + encodeURIComponent(jobId)).then(function (job) {
            if (job.state === 'NO_FILES') {
                track.classList.add('hidden');
                progressLabel.textContent = NO_FILES_TEXT;
                finish();
            } else if (job.state === 'FAILED') {
                progressLabel.textContent = job.message || 'Import failed.';
                MD.toast(job.message || 'Import failed.', 'error');
                finish();
            } else if (job.state === 'DONE') {
                showDone(job.results || []);
                finish();
            } else {
                if (job.total > 0 && job.current > 0) {
                    setProgress(job.current / job.total);
                    progressLabel.textContent =
                        'Importing ' + job.current + ' / ' + job.total + '  —  ' + job.fileName;
                } else if (job.total > 0) {
                    setProgress(0);
                    progressLabel.textContent = 'Importing — 0 / ' + job.total;
                }
                pollTimer = setTimeout(function () { poll(jobId); }, POLL_MS);
            }
        }).catch(function (err) {
            progressLabel.textContent = err.message;
            MD.toast(err.message, 'error');
            finish();
        });
    }

    /**
     * Resets the UI to the "Starting…" state before a scan or upload begins.
     */
    function begin() {
        busy = true;
        updateScanButton();
        resetProgress();
        track.classList.remove('hidden');
        progressLabel.textContent = 'Starting…';
    }

    /* ─── Start Scan (server folder) ────────────────────────── */

    /** Starts importing the folder typed in the path field. */
    function startScan() {
        const path = folderPath.value.trim();
        if (!path || busy) return;
        begin();
        MD.api.post('/api/import/scan', { path: path }).then(function (res) {
            poll(res.jobId);
        }).catch(function (err) {
            track.classList.add('hidden');
            progressLabel.textContent = err.message;
            MD.toast(err.message, 'error');
            finish();
        });
    }

    /* ─── Upload (drop zone / pickers) ──────────────────────── */

    /**
     * Tells whether a file name has a supported audio extension (same rule as the server).
     * @param {string} name file name
     * @returns {boolean} true for .mp3 and .m4a
     */
    function isAudio(name) {
        const n = name.toLowerCase();
        return n.endsWith('.mp3') || n.endsWith('.m4a');
    }

    /**
     * Filters entries to supported audio and uploads them, then polls the created job.
     * @param {Array<{file:File,path:string}>} entries files with their relative paths
     */
    function uploadEntries(entries) {
        if (busy) { MD.toast('An import is already running.', 'info'); return; }
        const audio = entries.filter(function (e) { return isAudio(e.file.name); });
        resetProgress();
        if (!audio.length) {
            progressLabel.textContent = NO_FILES_TEXT;
            return;
        }
        begin();
        const form = new FormData();
        audio.forEach(function (e) {
            form.append('files', e.file, e.file.name);
            form.append('paths', e.path);
        });
        MD.api.upload('/api/import/upload', form, function (f) {
            setProgress(f);
            progressLabel.textContent = 'Uploading… ' + Math.round(f * 100) + '%';
        }).then(function (res) {
            setProgress(0);
            progressLabel.textContent = 'Starting…';
            poll(res.jobId);
        }).catch(function (err) {
            track.classList.add('hidden');
            progressLabel.textContent = err.message;
            MD.toast(err.message, 'error');
            finish();
        });
    }

    /**
     * Reads every entry of a directory reader (readEntries returns batches).
     * @param {FileSystemDirectoryReader} reader the reader
     * @returns {Promise<FileSystemEntry[]>} all child entries
     */
    function readAll(reader) {
        return new Promise(function (resolve, reject) {
            const all = [];
            (function next() {
                reader.readEntries(function (batch) {
                    if (!batch.length) { resolve(all); return; }
                    all.push.apply(all, batch);
                    next();
                }, reject);
            })();
        });
    }

    /**
     * Walks a dropped file-system entry recursively, collecting audio files with relative paths.
     * @param {FileSystemEntry} entry a dropped file or directory
     * @param {string} prefix relative path of the parent folder, with trailing slash or empty
     * @param {Array<{file:File,path:string}>} out collector
     * @returns {Promise<void>} resolves when the entry has been fully walked
     */
    function walk(entry, prefix, out) {
        if (entry.isFile) {
            return new Promise(function (resolve) {
                entry.file(function (file) {
                    out.push({ file: file, path: prefix + entry.name });
                    resolve();
                }, function () { resolve(); });
            });
        }
        if (entry.isDirectory) {
            return readAll(entry.createReader()).then(function (children) {
                return Promise.all(children.map(function (c) {
                    return walk(c, prefix + entry.name + '/', out);
                }));
            }).catch(function () { /* unreadable folder: skip */ });
        }
        return Promise.resolve();
    }

    /**
     * Handles a drop on the drop zone: walks dropped folders and uploads the audio files.
     * @param {DragEvent} event the drop event
     */
    function handleDrop(event) {
        event.preventDefault();
        dropZone.classList.remove('drag-over');
        const dt = event.dataTransfer;
        if (!dt) return;
        const out = [];
        const items = dt.items ? Array.from(dt.items).filter(function (i) { return i.kind === 'file'; }) : [];
        if (items.length && items[0].webkitGetAsEntry) {
            // Entries must be taken synchronously, before the event ends.
            const entries = items.map(function (i) { return i.webkitGetAsEntry(); }).filter(Boolean);
            Promise.all(entries.map(function (e) { return walk(e, '', out); }))
                .then(function () { uploadEntries(out); });
        } else {
            Array.from(dt.files).forEach(function (f) { out.push({ file: f, path: f.name }); });
            uploadEntries(out);
        }
    }

    /**
     * Turns the files of a picker input into upload entries, using the folder-relative path when present.
     * @param {HTMLInputElement} input the file input
     */
    function handlePicked(input) {
        const entries = Array.from(input.files).map(function (f) {
            return { file: f, path: f.webkitRelativePath || f.name };
        });
        input.value = '';
        uploadEntries(entries);
    }

    /* ─── Wiring ────────────────────────────────────────────── */

    folderPath.addEventListener('input', updateScanButton);
    folderPath.addEventListener('keydown', function (e) { if (e.key === 'Enter') startScan(); });
    scanBtn.addEventListener('click', startScan);

    browseBtn.addEventListener('click', function (e) {
        MD.menu.show(e, [
            { label: 'Choose files…', onClick: function () { fileInput.click(); } },
            { label: 'Choose folder…', onClick: function () { folderInput.click(); } }
        ]);
    });
    fileInput.addEventListener('change', function () { handlePicked(fileInput); });
    folderInput.addEventListener('change', function () { handlePicked(folderInput); });

    dropZone.addEventListener('dragover', function (e) {
        e.preventDefault();
        dropZone.classList.add('drag-over');
    });
    dropZone.addEventListener('dragleave', function () { dropZone.classList.remove('drag-over'); });
    dropZone.addEventListener('drop', handleDrop);
    // A file dropped outside the zone must not make the browser navigate away.
    ['dragover', 'drop'].forEach(function (ev) {
        window.addEventListener(ev, function (e) { if (!dropZone.contains(e.target)) e.preventDefault(); });
    });

    updateScanButton();
})();
