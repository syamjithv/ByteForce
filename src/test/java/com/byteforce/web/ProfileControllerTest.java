package com.byteforce.web;

import com.byteforce.ByteForceWebApplication;
import com.byteforce.domain.Role;
import com.byteforce.domain.StudentProfile;
import com.byteforce.domain.User;
import com.byteforce.service.AuthService;
import com.byteforce.service.UserService;
import com.byteforce.web.util.WebSessionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(classes = ByteForceWebApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    private User registerTestUser(String email, String password, String name) {
        return authService.register(email, password, name);
    }

    @Test
    @DisplayName("Unauthenticated user accessing /profile is redirected to auth-required gate")
    void unauthenticatedUserCannotAccessProfile() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth-required?returnUrl=/profile&feature=managing your account and profile"));
    }

    @Test
    @DisplayName("Authenticated user can open /profile and see their details")
    void authenticatedUserCanOpenProfile() throws Exception {
        String email = "profile.view." + System.currentTimeMillis() + "@byteforce.com";
        User user = registerTestUser(email, "StrongPass123!", "Ada Lovelace");

        MockHttpSession session = new MockHttpSession();
        WebSessionUtil.setCurrentUser(session, user);

        mockMvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/index"))
                .andExpect(content().string(containsString("Ada Lovelace")))
                .andExpect(content().string(containsString(email)))
                .andExpect(content().string(containsString("Change Password")));
    }

    @Test
    @DisplayName("User can update their display name, college, and phone number")
    void userCanUpdateProfileDetails() throws Exception {
        String email = "profile.update." + System.currentTimeMillis() + "@byteforce.com";
        User user = registerTestUser(email, "StrongPass123!", "Initial Name");

        MockHttpSession session = new MockHttpSession();
        WebSessionUtil.setCurrentUser(session, user);

        mockMvc.perform(post("/profile/update")
                        .session(session)
                        .param("fullName", "Updated Ada Lovelace")
                        .param("college", "Cambridge University")
                        .param("graduationYear", "2026")
                        .param("phone", "+44-20-7946-0991"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("profileSuccessMessage"));

        StudentProfile updatedProfile = userService.getStudentProfile(user.getId()).orElseThrow();
        assertEquals("Updated Ada Lovelace", updatedProfile.getFullName());
        assertEquals("Cambridge University", updatedProfile.getCollege());
        assertEquals(2026, updatedProfile.getGraduationYear());
        assertEquals("+44-20-7946-0991", updatedProfile.getPhone());

        // Verify session user updated as well
        User sessionUser = WebSessionUtil.getCurrentUser(session).orElseThrow();
        assertEquals("Updated Ada Lovelace", sessionUser.getFullName());
    }

    @Test
    @DisplayName("Mascot endpoint is completely removed and returns 404")
    void mascotEndpointShouldReturnNotFound() throws Exception {
        String email = "mascot.test." + System.currentTimeMillis() + "@byteforce.com";
        User user = registerTestUser(email, "StrongPass123!", "Mascot Explorer");

        MockHttpSession session = new MockHttpSession();
        WebSessionUtil.setCurrentUser(session, user);

        mockMvc.perform(post("/profile/mascot")
                        .session(session)
                        .param("mascot", "cyber-runner"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User can upload a profile image and remove it cleanly")
    void userCanUploadAndRemoveAvatar() throws Exception {
        String email = "avatar.test." + System.currentTimeMillis() + "@byteforce.com";
        User user = registerTestUser(email, "StrongPass123!", "Avatar Candidate");

        MockHttpSession session = new MockHttpSession();
        WebSessionUtil.setCurrentUser(session, user);

        // 1x1 PNG image
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
                0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                0x42, 0x60, (byte) 0x82
        };

        MockMultipartFile file = new MockMultipartFile("avatarFile", "avatar.png", "image/png", pngBytes);

        mockMvc.perform(multipart("/profile/avatar")
                        .file(file)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("avatarSuccessMessage"));

        StudentProfile profile = userService.getStudentProfile(user.getId()).orElseThrow();
        assertNotNull(profile.getAvatarUrl());
        assertTrue(profile.getAvatarUrl().startsWith("data:image/png;base64,"));
        assertTrue(profile.hasCustomAvatar());

        // Remove avatar
        mockMvc.perform(post("/profile/avatar/remove")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("avatarSuccessMessage"));

        StudentProfile reloaded = userService.getStudentProfile(user.getId()).orElseThrow();
        assertNull(reloaded.getAvatarUrl());
        assertFalse(reloaded.hasCustomAvatar());
    }

    @Test
    @DisplayName("Password change rejects wrong current password, mismatched confirmation, and weak password")
    void passwordChangeValidationFailures() throws Exception {
        String email = "pwd.val." + System.currentTimeMillis() + "@byteforce.com";
        User user = registerTestUser(email, "InitialPass123!", "Sec Tester");

        MockHttpSession session = new MockHttpSession();
        WebSessionUtil.setCurrentUser(session, user);

        // 1. Wrong current password
        mockMvc.perform(post("/profile/password")
                        .session(session)
                        .param("currentPassword", "WrongPass999!")
                        .param("newPassword", "NewStrongPass456!")
                        .param("confirmPassword", "NewStrongPass456!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("passwordErrorMessage", "Current password is incorrect."));

        // 2. Mismatched confirmation
        mockMvc.perform(post("/profile/password")
                        .session(session)
                        .param("currentPassword", "InitialPass123!")
                        .param("newPassword", "NewStrongPass456!")
                        .param("confirmPassword", "DifferentPass789!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("passwordErrorMessage", "New password and confirmation do not match."));

        // 3. Weak new password
        mockMvc.perform(post("/profile/password")
                        .session(session)
                        .param("currentPassword", "InitialPass123!")
                        .param("newPassword", "weak")
                        .param("confirmPassword", "weak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("passwordErrorMessage"));
    }

    @Test
    @DisplayName("Successful password change updates hash, invalidates old credentials, and authenticates with new password")
    void successfulPasswordChangeEndToEnd() throws Exception {
        String email = "pwd.e2e." + System.currentTimeMillis() + "@byteforce.com";
        String oldPass = "OldStrongPass123!";
        String newPass = "BrandNewPass456!";
        User user = registerTestUser(email, oldPass, "Password Changer");

        MockHttpSession session = new MockHttpSession();
        WebSessionUtil.setCurrentUser(session, user);

        // Execute password change
        mockMvc.perform(post("/profile/password")
                        .session(session)
                        .param("currentPassword", oldPass)
                        .param("newPassword", newPass)
                        .param("confirmPassword", newPass))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("passwordSuccessMessage"));

        // Verify old password fails authentication
        assertThrows(Exception.class, () -> authService.login(email, oldPass));

        // Verify new password authenticates successfully
        User reauthenticated = authService.login(email, newPass);
        assertNotNull(reauthenticated);
        assertEquals(user.getId(), reauthenticated.getId());
    }

    @Test
    @DisplayName("User cannot modify another user's profile through request parameters")
    void cannotModifyAnotherUserProfile() throws Exception {
        String emailVictim = "victim." + System.currentTimeMillis() + "@byteforce.com";
        User victim = registerTestUser(emailVictim, "VictimPass123!", "Victim Student");

        String emailAttacker = "attacker." + System.currentTimeMillis() + "@byteforce.com";
        User attacker = registerTestUser(emailAttacker, "AttackerPass123!", "Attacker Student");

        MockHttpSession attackerSession = new MockHttpSession();
        WebSessionUtil.setCurrentUser(attackerSession, attacker);

        // Attempting to send someone else's ID or parameters
        mockMvc.perform(post("/profile/update")
                        .session(attackerSession)
                        .param("userId", victim.getId().toString()) // Malicious injection attempt
                        .param("fullName", "Hacked Name")
                        .param("college", "Hacked College"))
                .andExpect(status().is3xxRedirection());

        // Verify victim profile was NOT changed
        StudentProfile victimProfile = userService.getStudentProfile(victim.getId()).orElseThrow();
        assertEquals("Victim Student", victimProfile.getFullName());
        assertNull(victimProfile.getCollege());

        // Verify attacker profile was updated instead
        StudentProfile attackerProfile = userService.getStudentProfile(attacker.getId()).orElseThrow();
        assertEquals("Hacked Name", attackerProfile.getFullName());
    }
}
