package com.byteforce.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class DashboardPanel extends JPanel {
    public DashboardPanel() {
        super(new BorderLayout());
        add(new JLabel("Dashboard"), BorderLayout.CENTER);
    }
}
