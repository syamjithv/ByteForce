package com.byteforce.web.advice;

import com.byteforce.domain.User;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.byteforce.domain.StudentProfile;
import com.byteforce.service.UserService;

/**
 * Injects global model attributes available to all Thymeleaf templates:
 * - currentUser: authenticated user if present
 * - currentProfile: authenticated user's student profile (avatar) if present
 * - currentUri: the request path for active navigation styling
 * Also provides central handling for resource not found conditions (HTTP 404).
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;

    public GlobalModelAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public User currentUser(HttpSession session) {
        return WebSessionUtil.getCurrentUser(session).orElse(null);
    }

    @ModelAttribute("currentProfile")
    public StudentProfile currentProfile(HttpSession session) {
        return WebSessionUtil.getCurrentUser(session)
                .flatMap(u -> userService.getStudentProfile(u.getId()))
                .orElse(null);
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }
}

