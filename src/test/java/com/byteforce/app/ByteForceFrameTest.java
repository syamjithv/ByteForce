package com.byteforce.app;

import com.byteforce.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ByteForceFrameTest {

    @Mock
    private AuthService mockAuthService;

    @Test
    @DisplayName("Default constructor should construct frame successfully")
    void frameCanBeConstructed() {
        ByteForceFrame frame = new ByteForceFrame();
        assertNotNull(frame);
        assertEquals(ByteForceFrame.VIEW_LOGIN, frame.getCurrentView());
        assertNotNull(frame.getLoginPanel());
        assertNotNull(frame.getDashboardPanel());
        assertNotNull(frame.getPracticePanel());
        frame.dispose();
    }

    @Test
    @DisplayName("Constructor with AuthService should initialize with Login as active view")
    void frameCanBeConstructedWithAuthService() {
        ByteForceFrame frame = new ByteForceFrame(mockAuthService);
        assertNotNull(frame);
        assertEquals(ByteForceFrame.VIEW_LOGIN, frame.getCurrentView());
        assertNotNull(frame.getPracticePanel());
        frame.dispose();
    }

    @Test
    @DisplayName("Card navigation should transition between Login, Dashboard, and Practice views")
    void shouldSwitchViewsUsingCardNavigation() {
        ByteForceFrame frame = new ByteForceFrame(mockAuthService);

        assertEquals(ByteForceFrame.VIEW_LOGIN, frame.getCurrentView());

        frame.showDashboard();
        assertEquals(ByteForceFrame.VIEW_DASHBOARD, frame.getCurrentView());

        frame.showPractice();
        assertEquals(ByteForceFrame.VIEW_PRACTICE, frame.getCurrentView());

        frame.showLogin();
        assertEquals(ByteForceFrame.VIEW_LOGIN, frame.getCurrentView());

        frame.showView(ByteForceFrame.VIEW_DASHBOARD);
        assertEquals(ByteForceFrame.VIEW_DASHBOARD, frame.getCurrentView());

        frame.showView(ByteForceFrame.VIEW_PRACTICE);
        assertEquals(ByteForceFrame.VIEW_PRACTICE, frame.getCurrentView());

        frame.dispose();
    }
}
