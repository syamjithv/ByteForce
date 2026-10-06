package com.byteforce.web.controller;

import com.byteforce.domain.User;
import com.byteforce.exception.AuthenticationException;
import com.byteforce.exception.DuplicateUserException;
import com.byteforce.exception.ValidationException;
import com.byteforce.service.AuthService;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller handling user authentication (Login, Register, Logout)
 * delegating strictly to the core AuthService.
 */
@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "returnUrl", required = false, defaultValue = "/dashboard") String returnUrl,
                            @RequestParam(value = "registered", required = false) Boolean registered,
                            Model model,
                            HttpSession session) {
        if (WebSessionUtil.isAuthenticated(session)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("returnUrl", returnUrl);
        if (Boolean.TRUE.equals(registered)) {
            model.addAttribute("successMessage", "Account created successfully! Please sign in with your credentials.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam("email") String email,
                              @RequestParam("password") String password,
                              @RequestParam(value = "returnUrl", required = false, defaultValue = "/dashboard") String returnUrl,
                              Model model,
                              HttpSession session) {
        model.addAttribute("email", email);
        model.addAttribute("returnUrl", returnUrl);

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("errorMessage", "Please provide both email and password.");
            return "login";
        }

        try {
            User user = authService.login(email.trim(), password);
            WebSessionUtil.setCurrentUser(session, user);
            log.info("User {} logged in successfully via web interface", user.getEmail());

            if (returnUrl != null && !returnUrl.isBlank() && !returnUrl.equals("/login") && !returnUrl.equals("/register")) {
                return "redirect:" + returnUrl;
            }
            return "redirect:/dashboard";
        } catch (AuthenticationException e) {
            model.addAttribute("errorMessage", "Invalid email or password. Please try again.");
            return "login";
        } catch (ValidationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "login";
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            model.addAttribute("errorMessage", "An error occurred during sign-in. Please try again.");
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(value = "returnUrl", required = false, defaultValue = "/dashboard") String returnUrl,
                               Model model,
                               HttpSession session) {
        if (WebSessionUtil.isAuthenticated(session)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("returnUrl", returnUrl);
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam("fullName") String fullName,
                                 @RequestParam("email") String email,
                                 @RequestParam("password") String password,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 @RequestParam(value = "returnUrl", required = false, defaultValue = "/dashboard") String returnUrl,
                                 Model model,
                                 HttpSession session) {
        model.addAttribute("fullName", fullName);
        model.addAttribute("email", email);
        model.addAttribute("returnUrl", returnUrl);

        if (fullName == null || fullName.isBlank()) {
            model.addAttribute("errorMessage", "Please enter your full name.");
            return "register";
        }

        if (email == null || email.isBlank()) {
            model.addAttribute("errorMessage", "Please enter your email address.");
            return "register";
        }

        if (password == null || password.isBlank()) {
            model.addAttribute("errorMessage", "Please enter a password.");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match. Please re-enter.");
            return "register";
        }

        try {
            User registeredUser = authService.register(email.trim(), password, fullName.trim());
            log.info("User {} successfully registered via web interface", registeredUser.getEmail());

            // Authenticate directly for a frictionless onboarding journey
            try {
                User user = authService.login(email.trim(), password);
                WebSessionUtil.setCurrentUser(session, user);
                return "redirect:/dashboard?welcome=true";
            } catch (Exception loginEx) {
                log.warn("Auto-login after registration failed, redirecting to login page", loginEx);
                return "redirect:/login?registered=true";
            }

        } catch (DuplicateUserException e) {
            model.addAttribute("errorMessage", "An account with this email address already exists. Please sign in instead.");
            return "register";
        } catch (ValidationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            model.addAttribute("errorMessage", "Failed to create account. Please ensure password has at least 8 characters with letters, numbers, and symbols.");
            return "register";
        }
    }

    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        WebSessionUtil.clear(session);
        try {
            authService.logout();
        } catch (Exception ignored) {
        }
        return "redirect:/?loggedOut=true";
    }
}
