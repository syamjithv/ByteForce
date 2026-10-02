package com.byteforce.web.controller;

import com.byteforce.domain.StudentProfile;
import com.byteforce.domain.User;
import com.byteforce.exception.AuthenticationException;
import com.byteforce.exception.ValidationException;
import com.byteforce.service.UserService;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Controller for the authenticated Account & Profile management experience.
 * Manages personal info, persistent profile pictures, and secure password changes.
 */
@Controller
@RequestMapping({"/profile", "/account"})
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private static final long MAX_AVATAR_SIZE_BYTES = 2 * 1024 * 1024; // 2 MB limit
    private static final int AVATAR_TARGET_DIMENSION = 256; // 256x256 pixels

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String viewProfile(Model model, HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/profile&feature=managing your account and profile";
        }

        User user = userOpt.get();
        StudentProfile profile = userService.getStudentProfile(user.getId())
                .orElseGet(() -> userService.updateStudentProfile(user.getId(), user.getFullName(), null, null, null));

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);

        return "profile/index";
    }

    @PostMapping("/update")
    public String updateProfileDetails(@RequestParam("fullName") String fullName,
                                       @RequestParam(value = "phone", required = false) String phone,
                                       @RequestParam(value = "college", required = false) String college,
                                       @RequestParam(value = "graduationYear", required = false) Integer graduationYear,
                                       RedirectAttributes redirectAttributes,
                                       HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/profile";
        }

        User user = userOpt.get();
        if (fullName == null || fullName.isBlank()) {
            redirectAttributes.addFlashAttribute("profileErrorMessage", "Full name must not be blank.");
            return "redirect:/profile";
        }

        try {
            userService.updateStudentProfile(user.getId(), fullName.trim(), phone, college, graduationYear);

            // Update user in session so topbar and sidebar immediately reflect new name
            User refreshedUser = userService.getUserById(user.getId()).orElse(user);
            WebSessionUtil.setCurrentUser(session, refreshedUser);

            redirectAttributes.addFlashAttribute("profileSuccessMessage", "Profile information updated successfully.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("profileErrorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update profile for user {}", user.getId(), e);
            redirectAttributes.addFlashAttribute("profileErrorMessage", "An error occurred while saving your profile.");
        }

        return "redirect:/profile";
    }

    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/profile";
        }

        User user = userOpt.get();

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("avatarErrorMessage", "Please select an image file to upload.");
            return "redirect:/profile";
        }

        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            redirectAttributes.addFlashAttribute("avatarErrorMessage", "Profile image must be less than 2 MB in size.");
            return "redirect:/profile";
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equalsIgnoreCase("image/jpeg") &&
                                    !contentType.equalsIgnoreCase("image/jpg") &&
                                    !contentType.equalsIgnoreCase("image/png") &&
                                    !contentType.equalsIgnoreCase("image/webp"))) {
            redirectAttributes.addFlashAttribute("avatarErrorMessage", "Only JPG, PNG, and WebP images are supported.");
            return "redirect:/profile";
        }

        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                redirectAttributes.addFlashAttribute("avatarErrorMessage", "Invalid or corrupted image file.");
                return "redirect:/profile";
            }

            // Square crop & resize to standard dimension
            BufferedImage resizedAvatar = resizeAndSquareCrop(originalImage, AVATAR_TARGET_DIMENSION);

            // Convert to deployment-safe data URL
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedAvatar, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String avatarDataUrl = "data:image/png;base64," + base64Image;

            userService.updateAvatar(user.getId(), avatarDataUrl);
            redirectAttributes.addFlashAttribute("avatarSuccessMessage", "Profile picture updated successfully.");
        } catch (IOException e) {
            log.error("Failed to process avatar upload for user {}", user.getId(), e);
            redirectAttributes.addFlashAttribute("avatarErrorMessage", "Failed to process image. Please try another file.");
        } catch (Exception e) {
            log.error("Error saving avatar for user {}", user.getId(), e);
            redirectAttributes.addFlashAttribute("avatarErrorMessage", "An unexpected error occurred while saving your avatar.");
        }

        return "redirect:/profile";
    }

    @PostMapping("/avatar/remove")
    public String removeAvatar(RedirectAttributes redirectAttributes, HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/profile";
        }

        User user = userOpt.get();
        userService.updateAvatar(user.getId(), null);
        redirectAttributes.addFlashAttribute("avatarSuccessMessage", "Profile picture removed. Default avatar is now active.");

        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/profile";
        }

        User user = userOpt.get();

        if (currentPassword == null || currentPassword.isBlank() ||
            newPassword == null || newPassword.isBlank() ||
            confirmPassword == null || confirmPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("passwordErrorMessage", "All password fields are required.");
            return "redirect:/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordErrorMessage", "New password and confirmation do not match.");
            return "redirect:/profile";
        }

        try {
            userService.changePassword(user.getId(), currentPassword, newPassword);

            // Update user in session with new password hash
            User refreshedUser = userService.getUserById(user.getId()).orElse(user);
            WebSessionUtil.setCurrentUser(session, refreshedUser);

            redirectAttributes.addFlashAttribute("passwordSuccessMessage", "Password changed successfully.");
        } catch (AuthenticationException e) {
            redirectAttributes.addFlashAttribute("passwordErrorMessage", "Current password is incorrect.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("passwordErrorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to change password for user {}", user.getId(), e);
            redirectAttributes.addFlashAttribute("passwordErrorMessage", "Failed to update password. Please try again.");
        }

        return "redirect:/profile";
    }

    /**
     * Centers and scales an image into a crisp square avatar.
     */
    private BufferedImage resizeAndSquareCrop(BufferedImage original, int targetSize) {
        int width = original.getWidth();
        int height = original.getHeight();
        int cropSize = Math.min(width, height);
        int cropX = (width - cropSize) / 2;
        int cropY = (height - cropSize) / 2;

        BufferedImage cropped = original.getSubimage(cropX, cropY, cropSize, cropSize);

        BufferedImage output = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = output.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(cropped, 0, 0, targetSize, targetSize, null);
        g2d.dispose();

        return output;
    }
}
