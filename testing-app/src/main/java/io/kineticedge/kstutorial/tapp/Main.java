package io.kineticedge.kstutorial.tapp;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Window("Blue Window", Color.BLUE, 100, 100);
            new Window("Silver Window", Color.LIGHT_GRAY, 550, 100);
        });

    }
}
