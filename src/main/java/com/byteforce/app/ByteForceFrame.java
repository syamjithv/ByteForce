package com.byteforce.app;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class ByteForceFrame extends JFrame {

    public ByteForceFrame() {
        setTitle("ByteForce");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(new JPanel(new BorderLayout()) {{
            add(new JLabel("ByteForce scaffold is ready"), BorderLayout.CENTER);
        }}, BorderLayout.CENTER);
        pack();
    }
}
