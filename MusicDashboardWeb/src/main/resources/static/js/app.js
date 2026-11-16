/*
 * app.js - shared helper library for every Music Dashboard page.
 *
 * Defines window.MD with:
 *   MD.api    fetch/XHR wrappers (JSON, CSRF header, session-expiry redirect)
 *   MD.toast  stacked, auto-dismissing notifications
 *   MD.modal  dialogs (open / confirm / alert) replacing the desktop AppUtils dialogs
 *   MD.menu   popup / context menus
 *   MD.fmt    time and duration formatting
 *   MD.esc    HTML escaping
 *   MD.artUrl album art URL builder
 *   MD.el     tiny DOM builder
 *   MD.touch  touch detection (single tap plays where a mouse needs a double-click)
 *   MD.drawer the sidebar as an off-canvas drawer below 1280px (opened by the top bar's menu button)
 *
 * MD.player is defined separately in player.js.
 */
(function () {
    'use strict';

    const MD = window.MD || {};
    window.MD = MD;

    /* ─── Small helpers ─────────────────────────────────────── */

    /**
     * Reads the content attribute of a named meta tag.
     * @param {string} name the meta tag name
     * @returns {string|null} the content, or null when the tag is absent
     */
    function meta(name) {
        const m = document.querySelector('meta[name="' + name + '"]');
        return m ? m.getAttribute('content') : null;
    }

    /**
     * Builds the headers every API request carries (CSRF token and AJAX marker).
     * @param {boolean} json whether to add a JSON Content-Type
     * @returns {Object<string,string>} the header map
     */
    function baseHeaders(json) {
        const h = {'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json'};
        const name = meta('_csrf_header');
        const token = meta('_csrf');
        if (name && token) h[name] = token;
        if (json) h['Content-Type'] = 'application/json';
        return h;
    }

    /**
     * Extracts a human-readable message from an error response body.
     * @param {string} text the raw response text
     * @param {string} fallback message used when none can be found
     * @returns {string} the message
     */
    function errorMessage(text, fallback) {
        if (text) {
            try {
                const data = JSON.parse(text);
                const msg = data && (data.message || data.error || data.detail);
                if (msg) return String(msg);
            } catch (e) {
                /* not JSON - fall through */
            }
        }
        return fallback;
    }

    /**
     * Sends the browser to the login page when a response indicates an expired session.
     * @param {string} url the final response URL
     * @returns {boolean} true when a redirect to /login was triggered
     */
    function redirectedToLogin(url) {
        if (!url) return false;
        const path = new URL(url, location.href).pathname;
        if (path === '/login' && location.pathname !== '/login') {
            window.location.href = '/login';
            return true;
        }
        return false;
    }

    /**
     * Performs a fetch and converts the result to parsed JSON / null / rejection.
     * @param {string} url the request URL
     * @param {string} method the HTTP method
     * @param {*} [body] optional JSON-serialisable body
     * @returns {Promise<any>} parsed JSON, or null for 204 / empty bodies
     */
    async function request(url, method, body) {
        const opts = {method: method, headers: baseHeaders(body !== undefined), credentials: 'same-origin'};
        if (body !== undefined) opts.body = JSON.stringify(body);
        const res = await fetch(url, opts);
        if (res.redirected && redirectedToLogin(res.url)) {
            return new Promise(function () { /* page is navigating away */ });
        }
        const text = await res.text();
        if (!res.ok) throw new Error(errorMessage(text, res.statusText || ('HTTP ' + res.status)));
        if (res.status === 204 || !text) return null;
        try {
            return JSON.parse(text);
        } catch (e) {
            return text;
        }
    }

    /* ─── MD.api ────────────────────────────────────────────── */
    MD.api = {
        /**
         * GET a URL.
         * @param {string} url the request URL
         * @returns {Promise<any>} parsed JSON or null
         */
        get: function (url) { return request(url, 'GET'); },

        /**
         * POST with an optional JSON body.
         * @param {string} url the request URL
         * @param {*} [body] JSON-serialisable body
         * @returns {Promise<any>} parsed JSON or null
         */
        post: function (url, body) { return request(url, 'POST', body); },

        /**
         * PUT with an optional JSON body.
         * @param {string} url the request URL
         * @param {*} [body] JSON-serialisable body
         * @returns {Promise<any>} parsed JSON or null
         */
        put: function (url, body) { return request(url, 'PUT', body); },

        /**
         * DELETE a URL.
         * @param {string} url the request URL
         * @returns {Promise<any>} parsed JSON or null
         */
        del: function (url) { return request(url, 'DELETE'); },

        /**
         * Multipart POST using XMLHttpRequest so upload progress can be reported.
         * @param {string} url the request URL
         * @param {FormData} formData the form data to send
         * @param {function(number):void} [onProgress] called with a fraction between 0 and 1
         * @returns {Promise<any>} parsed JSON or null
         */
        upload: function (url, formData, onProgress) {
            return new Promise(function (resolve, reject) {
                const xhr = new XMLHttpRequest();
                xhr.open('POST', url);
                const headers = baseHeaders(false);
                Object.keys(headers).forEach(function (k) { xhr.setRequestHeader(k, headers[k]); });
                if (onProgress && xhr.upload) {
                    xhr.upload.onprogress = function (e) {
                        if (e.lengthComputable) onProgress(e.loaded / e.total);
                    };
                }
                xhr.onload = function () {
                    if (xhr.responseURL && redirectedToLogin(xhr.responseURL)) return;
                    const text = xhr.responseText;
                    if (xhr.status < 200 || xhr.status >= 300) {
                        reject(new Error(errorMessage(text, xhr.statusText || ('HTTP ' + xhr.status))));
                        return;
                    }
                    if (onProgress) onProgress(1);
                    if (xhr.status === 204 || !text) { resolve(null); return; }
                    try { resolve(JSON.parse(text)); } catch (e) { resolve(text); }
                };
                xhr.onerror = function () { reject(new Error('Network error')); };
                xhr.send(formData);
            });
        }
    };

    /* ─── MD.esc / MD.artUrl / MD.fmt ───────────────────────── */

    /**
     * HTML-escapes a value for safe use in innerHTML.
     * @param {*} text the value (null/undefined become an empty string)
     * @returns {string} the escaped string
     */
    MD.esc = function (text) {
        return String(text == null ? '' : text)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    };

    /**
     * Builds the album art URL.
     * @param {number|string} albumId the album id
     * @returns {string} "/media/art/album/{albumId}"
     */
    MD.artUrl = function (albumId) { return '/media/art/album/' + encodeURIComponent(albumId); };

    MD.fmt = {
        /**
         * Formats seconds as m:ss (83.4 gives "1:23").
         * @param {number} seconds the time in seconds
         * @returns {string} the formatted time
         */
        time: function (seconds) {
            const total = Math.max(0, Math.floor(Number(seconds) || 0));
            const s = total % 60;
            return Math.floor(total / 60) + ':' + (s < 10 ? '0' : '') + s;
        },

        /**
         * Formats a song duration; null or non-positive gives "--:--".
         * @param {number|null} seconds the duration in seconds
         * @returns {string} the formatted duration
         */
        duration: function (seconds) {
            if (seconds == null || !(Number(seconds) > 0)) return '--:--';
            return MD.fmt.time(seconds);
        }
    };

    /* ─── MD.el ─────────────────────────────────────────────── */

    /**
     * Tiny DOM builder.
     * attrs: class, text, html, dataset {k:v}, style (string or object), on {event:fn},
     * or any other attribute / property (true sets a boolean attribute, false/null skips it).
     * @param {string} tag the element tag name
     * @param {Object} [attrs] attributes and options
     * @param {Array|Node|string} [children] child nodes or strings
     * @returns {HTMLElement} the new element
     */
    MD.el = function (tag, attrs, children) {
        const node = document.createElement(tag);
        Object.keys(attrs || {}).forEach(function (k) {
            const v = attrs[k];
            if (v == null || v === false) return;
            if (k === 'class' || k === 'className') node.className = v;
            else if (k === 'text') node.textContent = v;
            else if (k === 'html') node.innerHTML = v;
            else if (k === 'dataset') Object.keys(v).forEach(function (d) { node.dataset[d] = v[d]; });
            else if (k === 'style' && typeof v === 'object') Object.assign(node.style, v);
            else if (k === 'on') Object.keys(v).forEach(function (ev) { node.addEventListener(ev, v[ev]); });
            else if (v === true) node.setAttribute(k, '');
            else node.setAttribute(k, v);
        });
        const list = children == null ? [] : (Array.isArray(children) ? children : [children]);
        list.forEach(function (c) {
            if (c == null || c === false) return;
            node.appendChild(c instanceof Node ? c : document.createTextNode(String(c)));
        });
        return node;
    };

    /* ─── MD.touch ──────────────────────────────────────────── */

    /** Pointer type of the most recent press: 'mouse', 'touch', 'pen', or '' before any press. */
    let lastPointerType = '';
    document.addEventListener('pointerdown', function (e) { lastPointerType = e.pointerType || ''; }, true);

    MD.touch = {
        /**
         * Tells whether the device's main pointer is a finger (the CSS media feature "pointer: coarse").
         * @returns {boolean} true on phones and tablets, false with a mouse or trackpad
         */
        coarse: function () {
            return !!(window.matchMedia && window.matchMedia('(pointer: coarse)').matches);
        },

        /**
         * Tells whether a click should count as a touch tap: the device's main pointer is a finger,
         * or this particular click came from a finger or pen (touch-screen laptops). Pages use it to
         * play a song on a single tap where a mouse needs a double-click.
         * @param {Event} [e] the click or dblclick event
         * @returns {boolean} true for a tap, false for a mouse click
         */
        isTap: function (e) {
            const type = (e && e.pointerType) || lastPointerType;
            return type === 'touch' || type === 'pen' || MD.touch.coarse();
        }
    };

    /* ─── MD.drawer ─────────────────────────────────────────── */

    /** Media query that is true when the desktop layout (fixed sidebar) is showing. */
    const DESKTOP_QUERY = '(min-width: 1280px)';

    /**
     * Finds the sidebar element of the page.
     * @returns {HTMLElement|null} the sidebar, or null on pages without one (sign-in, error)
     */
    function sidebarEl() {
        return document.getElementById('app-sidebar') || document.querySelector('.sidebar');
    }

    /**
     * Tells whether the drawer is open.
     * @returns {boolean} true while <body> carries the sidebar-open class
     */
    function drawerOpen() {
        return document.body.classList.contains('sidebar-open');
    }

    /**
     * Opens or closes the sidebar drawer: toggles the sidebar-open class on <body> (the CSS does the
     * sliding), keeps the menu button's ARIA state in step and moves focus into / out of the drawer.
     * @param {boolean} open true to open, false to close
     * @returns {void}
     */
    function setDrawer(open) {
        const sidebar = sidebarEl();
        if (!sidebar || open === drawerOpen()) return;
        const btn = document.querySelector('[data-action="toggle-sidebar"]');
        document.body.classList.toggle('sidebar-open', open);
        if (btn) {
            btn.setAttribute('aria-expanded', open ? 'true' : 'false');
            btn.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
        }
        if (open) {
            const first = sidebar.querySelector('a[href], button:not(:disabled), select');
            if (first) first.focus({preventScroll: true});
        } else if (btn && sidebar.contains(document.activeElement)) {
            btn.focus({preventScroll: true});
        }
    }

    MD.drawer = {
        /** Opens the sidebar drawer (no effect at desktop width, where the sidebar is always visible). */
        open: function () { setDrawer(true); },
        /** Closes the sidebar drawer. */
        close: function () { setDrawer(false); },
        /** Opens the drawer when it is closed and closes it when it is open. */
        toggle: function () { setDrawer(!drawerOpen()); }
    };

    /**
     * Wires the drawer: the menu button toggles it, a tap on the backdrop or Escape closes it, and it
     * closes itself when the window grows to desktop width.
     * @returns {void}
     */
    function initDrawer() {
        document.addEventListener('click', function (e) {
            if (!e.target.closest) return;
            if (e.target.closest('[data-action="toggle-sidebar"]')) MD.drawer.toggle();
            else if (e.target.closest('[data-action="close-sidebar"]')) MD.drawer.close();
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && drawerOpen()) MD.drawer.close();
        });
        if (window.matchMedia) {
            const desktop = window.matchMedia(DESKTOP_QUERY);
            /** Closes the drawer once the desktop layout takes over. */
            const onChange = function () { if (desktop.matches) MD.drawer.close(); };
            if (desktop.addEventListener) desktop.addEventListener('change', onChange);
            else if (desktop.addListener) desktop.addListener(onChange);
        }
    }
    initDrawer();

    /* ─── MD.toast ──────────────────────────────────────────── */

    /**
     * Returns the fixed container that stacks toasts, creating it on first use.
     * @returns {HTMLElement} the toast container
     */
    function toastContainer() {
        let c = document.querySelector('.md-toast-container');
        if (!c) {
            c = MD.el('div', {class: 'md-toast-container', 'aria-live': 'polite'});
            document.body.appendChild(c);
        }
        return c;
    }

    /**
     * Shows a toast that auto-dismisses.
     * @param {string} message the text to show
     * @param {'info'|'success'|'error'} [type='info'] the toast style
     * @returns {void}
     */
    MD.toast = function (message, type) {
        const kind = type === 'success' || type === 'error' ? type : 'info';
        const t = MD.el('div', {class: 'md-toast md-toast-' + kind, role: kind === 'error' ? 'alert' : 'status', text: message});
        toastContainer().appendChild(t);
        requestAnimationFrame(function () { t.classList.add('md-toast-show'); });
        let gone = false;
        const dismiss = function () {
            if (gone) return;
            gone = true;
            t.classList.remove('md-toast-show');
            setTimeout(function () { t.remove(); }, 250);
        };
        t.addEventListener('click', dismiss);
        setTimeout(dismiss, kind === 'error' ? 6000 : 3500);
    };

    /* ─── MD.modal ──────────────────────────────────────────── */

    /**
     * Creates a button element for a dialog footer.
     * @param {{label:string, className:string, onClick:function}} b the button description
     * @param {function(): void} close closes the dialog
     * @returns {HTMLButtonElement} the button
     */
    function modalButton(b, close) {
        return MD.el('button', {
            type: 'button',
            class: b.className || 'btn-blue',
            text: b.label,
            style: {padding: '0 18px'},
            on: {click: function () { if (b.onClick) b.onClick(close); else close(); }}
        });
    }

    MD.modal = {
        /**
         * Opens a modal dialog. Escape and a backdrop click close it; focus moves into it.
         * @param {{title:string, content:(HTMLElement|string), buttons:Array, wide:boolean, onClose:function}} opts
         *        buttons: [{label, className, onClick(close)}]; content may be a node or HTML string
         * @returns {{el: HTMLElement, close: function(): void}} the dialog handle
         */
        open: function (opts) {
            opts = opts || {};
            const previouslyFocused = document.activeElement;
            const body = MD.el('div', {class: 'md-modal-body'});
            if (opts.content instanceof Node) body.appendChild(opts.content);
            else if (opts.content != null) body.innerHTML = opts.content;

            const dialog = MD.el('div', {
                class: 'md-modal' + (opts.wide ? ' md-modal-wide' : ''),
                role: 'dialog', 'aria-modal': 'true', tabindex: '-1'
            });
            if (opts.title) dialog.appendChild(MD.el('div', {class: 'md-modal-header', text: opts.title}));
            dialog.appendChild(body);

            const backdrop = MD.el('div', {class: 'md-modal-backdrop'}, dialog);
            let closed = false;
            const onKey = function (e) {
                if (e.key === 'Escape') { e.stopPropagation(); close(); }
            };
            /** Closes the dialog once, restores focus and calls onClose. */
            function close() {
                if (closed) return;
                closed = true;
                document.removeEventListener('keydown', onKey, true);
                backdrop.remove();
                if (previouslyFocused && previouslyFocused.focus) previouslyFocused.focus();
                if (opts.onClose) opts.onClose();
            }
            if (opts.buttons && opts.buttons.length) {
                dialog.appendChild(MD.el('div', {class: 'md-modal-footer'},
                    opts.buttons.map(function (b) { return modalButton(b, close); })));
            }
            backdrop.addEventListener('mousedown', function (e) { if (e.target === backdrop) close(); });
            document.addEventListener('keydown', onKey, true);
            document.body.appendChild(backdrop);

            const focusable = dialog.querySelector('input, select, textarea, button, [tabindex]:not([tabindex="-1"])');
            (focusable || dialog).focus();
            return {el: dialog, close: close};
        },

        /**
         * Shows a confirmation dialog.
         * @param {string} message the question
         * @param {{title:string, confirmLabel:string, cancelLabel:string, danger:boolean}} [opts] options
         * @returns {Promise<boolean>} true when confirmed, false when cancelled or dismissed
         */
        confirm: function (message, opts) {
            opts = opts || {};
            return new Promise(function (resolve) {
                let answer = false;
                MD.modal.open({
                    title: opts.title || 'Confirm',
                    content: MD.el('div', {text: message}),
                    buttons: [
                        {label: opts.cancelLabel || 'Cancel', className: 'btn-blue', onClick: function (close) { close(); }},
                        {label: opts.confirmLabel || 'OK', className: opts.danger ? 'btn-red' : 'green-btn',
                            onClick: function (close) { answer = true; close(); }}
                    ],
                    onClose: function () { resolve(answer); }
                });
            });
        },

        /**
         * Shows an informational / error dialog with a single OK button.
         * @param {string} message the text to show
         * @param {string} [title='Error'] the dialog title
         * @returns {Promise<void>} resolves when the dialog is closed
         */
        alert: function (message, title) {
            return new Promise(function (resolve) {
                MD.modal.open({
                    title: title || 'Error',
                    content: MD.el('div', {text: message}),
                    buttons: [{label: 'OK', className: 'green-btn', onClick: function (close) { close(); }}],
                    onClose: resolve
                });
            });
        }
    };

    /* ─── MD.menu ───────────────────────────────────────────── */

    let openMenu = null;

    /** Closes the currently open popup menu, if any, and removes its listeners. */
    function closeMenu() {
        if (!openMenu) return;
        const m = openMenu;
        openMenu = null;
        m.remove();
        document.removeEventListener('mousedown', onOutside, true);
        document.removeEventListener('keydown', onMenuKey, true);
        window.removeEventListener('scroll', closeMenu, true);
        window.removeEventListener('resize', closeMenu);
        window.removeEventListener('blur', closeMenu);
    }

    /**
     * Closes the menu on a mouse press outside it.
     * @param {MouseEvent} e the mousedown event
     */
    function onOutside(e) {
        if (openMenu && !openMenu.contains(e.target)) closeMenu();
    }

    /**
     * Closes the menu on Escape.
     * @param {KeyboardEvent} e the keydown event
     */
    function onMenuKey(e) {
        if (e.key === 'Escape') { e.stopPropagation(); closeMenu(); }
    }

    /**
     * Shows a popup / context menu at an element or mouse event, kept inside the viewport.
     * Closes on item click, outside click, Escape, scroll or resize.
     * @param {HTMLElement|MouseEvent} anchorOrEvent element to anchor under, or a mouse event
     * @param {Array<{label:string, onClick:function, danger:boolean, disabled:boolean}>} items menu items
     * @returns {void}
     */
    MD.menu = {
        show: function (anchorOrEvent, items) {
            closeMenu();
            if (anchorOrEvent && anchorOrEvent.preventDefault) anchorOrEvent.preventDefault();
            const menu = MD.el('div', {class: 'md-menu', role: 'menu'},
                (items || []).map(function (it) {
                    return MD.el('button', {
                        type: 'button', role: 'menuitem',
                        class: 'md-menu-item' + (it.danger ? ' md-menu-item-danger' : ''),
                        text: it.label, disabled: !!it.disabled,
                        on: {click: function () { closeMenu(); if (it.onClick) it.onClick(); }}
                    });
                }));
            menu.style.visibility = 'hidden';
            document.body.appendChild(menu);
            openMenu = menu;

            let x, y;
            if (anchorOrEvent instanceof Element) {
                const r = anchorOrEvent.getBoundingClientRect();
                x = r.left;
                y = r.bottom + 2;
            } else {
                x = anchorOrEvent.clientX;
                y = anchorOrEvent.clientY;
            }
            const w = menu.offsetWidth, h = menu.offsetHeight;
            if (x + w > window.innerWidth - 4) x = Math.max(4, window.innerWidth - w - 4);
            if (y + h > window.innerHeight - 4) {
                if (anchorOrEvent instanceof Element) y = Math.max(4, anchorOrEvent.getBoundingClientRect().top - h - 2);
                else y = Math.max(4, window.innerHeight - h - 4);
            }
            menu.style.left = x + 'px';
            menu.style.top = y + 'px';
            menu.style.visibility = 'visible';

            document.addEventListener('mousedown', onOutside, true);
            document.addEventListener('keydown', onMenuKey, true);
            window.addEventListener('scroll', closeMenu, true);
            window.addEventListener('resize', closeMenu);
            window.addEventListener('blur', closeMenu);
        }
    };
})();
