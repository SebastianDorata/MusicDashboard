package com.sebastiandorata.musicdashboard.controller.Authentication;

import com.sebastiandorata.musicdashboard.service.AuthenticationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Web controller for the sign-in and create-account page.
 *
 * <p>Renders {@code login.html} with the login card and the registration card side by side, like
 * the desktop screen. Login itself is handled by Spring Security ({@code POST /login}, see
 * {@link SecurityConfig}); this controller shows the one-time login error or logout notice and
 * implements {@code POST /register}, a faithful port of the desktop {@code handleSignup}.
 *
 * <p>Model attributes read by the template: {@code loginMessage}, {@code loginSuccess},
 * {@code signupMessage}, {@code signupSuccess}, {@code signupUsername}, {@code signupEmail}.
 *
 * <p>SRP: page rendering and registration input handling only; credential rules live in
 * {@link AuthenticationService}.
 */
@Controller
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Shows the login page, or redirects to the dashboard when already logged in.
     *
     * @param session the HTTP session holding the one-time login error message
     * @param logout  present when the user was just logged out
     * @param model   the view model
     * @return the {@code login} template, or a redirect to {@code /dashboard}
     */
    @GetMapping("/login")
    public String login(HttpSession session, @RequestParam(required = false) String logout, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        Object error = session.getAttribute(SecurityConfig.LOGIN_ERROR_ATTRIBUTE);
        session.removeAttribute(SecurityConfig.LOGIN_ERROR_ATTRIBUTE);
        if (error != null) {
            model.addAttribute("loginMessage", error.toString());
            model.addAttribute("loginSuccess", false);
        } else if (logout != null) {
            model.addAttribute("loginMessage", "You have been signed out.");
            model.addAttribute("loginSuccess", true);
        }
        return "login";
    }

    /**
     * Handles a registration submission, same rules and messages as the desktop signup card.
     *
     * <p>Validates that username and password are non-empty and the password has at least 6
     * characters, then calls {@link AuthenticationService#register(String, String, String)}.
     * Always redirects back to {@code /login} with the outcome flashed to the signup card; typed
     * values (never the password) are flashed back on failure.
     *
     * @param username the submitted username
     * @param email    the submitted email
     * @param password the submitted password
     * @param ra       flash attributes for the redirect
     * @return a redirect to {@code /login}
     */
    @PostMapping("/register")
    public String register(@RequestParam(defaultValue = "") String username,
                           @RequestParam(defaultValue = "") String email,
                           @RequestParam(defaultValue = "") String password,
                           RedirectAttributes ra) {
        String name = username.trim();
        String mail = email.trim();

        if (name.isEmpty() || password.isEmpty()) {
            return fail(ra, "Please enter a username and password.", name, mail);
        }
        if (password.length() < 6) {
            return fail(ra, "Password must be at least 6 characters.", name, mail);
        }
        try {
            authenticationService.register(mail, password, name);
            ra.addFlashAttribute("signupMessage", "Account created! You can now sign in.");
            ra.addFlashAttribute("signupSuccess", true);
        } catch (IllegalArgumentException e) {
            return fail(ra, e.getMessage(), name, mail);
        } catch (Exception e) {
            return fail(ra, "Registration failed: " + e.getMessage(), name, mail);
        }
        return "redirect:/login";
    }

    /**
     * Flashes a signup error plus the typed values and redirects to the login page.
     *
     * @param ra       flash attributes
     * @param message  the error message
     * @param username the typed username
     * @param email    the typed email
     * @return a redirect to {@code /login}
     */
    private String fail(RedirectAttributes ra, String message, String username, String email) {
        ra.addFlashAttribute("signupMessage", message);
        ra.addFlashAttribute("signupSuccess", false);
        ra.addFlashAttribute("signupUsername", username);
        ra.addFlashAttribute("signupEmail", email);
        return "redirect:/login";
    }
}
