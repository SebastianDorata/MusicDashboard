/*
 * login.js - behaviour of the sign-in / create-account page.
 *
 * The two cards are always visible side by side (like the desktop screen), so there is no card
 * switching. This script only mirrors the desktop button behaviour: while the login form is
 * submitted the button is disabled and a "Signing in..." message is shown (the server delays
 * failed attempts by 5 seconds), and empty fields are rejected with the desktop message.
 */
(function () {
    'use strict';

    /**
     * Shows a feedback message in the given card, creating the label when needed.
     * @param {HTMLElement} form    the card (form) element
     * @param {string} id           id for the feedback element
     * @param {string} text         message text
     * @param {boolean} success     true for the success style, false for the error style
     * @returns {void}
     */
    function showFeedback(form, id, text, success) {
        var label = document.getElementById(id);
        if (!label) {
            label = document.createElement('div');
            label.id = id;
            form.insertBefore(label, form.querySelector('button[type=submit]'));
        }
        label.textContent = text;
        label.className = success ? 'login-success-label' : 'login-error-label';
    }

    /**
     * Wires the login form: validates non-empty fields, then disables the button while submitting.
     * @returns {void}
     */
    function initLoginForm() {
        var form = document.getElementById('login-form');
        if (!form) return;
        form.addEventListener('submit', function (e) {
            var user = form.elements['username'].value.trim();
            var pass = form.elements['password'].value;
            if (!user || !pass) {
                e.preventDefault();
                showFeedback(form, 'login-feedback', 'Please enter your username and password.', false);
                return;
            }
            showFeedback(form, 'login-feedback', 'Signing in...', true);
            // Disable after the submit event so the form data is still sent.
            setTimeout(function () { document.getElementById('login-btn').disabled = true; }, 0);
        });
    }

    /**
     * Restores the login button when the page is shown again from the back/forward cache.
     * @returns {void}
     */
    function initPageShow() {
        window.addEventListener('pageshow', function (e) {
            if (e.persisted) {
                var btn = document.getElementById('login-btn');
                if (btn) btn.disabled = false;
            }
        });
    }

    initLoginForm();
    initPageShow();
})();
