package io.kineticedge.kstutorial.tapp;

// BlueAndSilverWindows.java

import oshi.SystemInfo;
import oshi.software.os.OSDesktopWindow;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class Window {

    private static final String TEMPLATE = """
                <html>
                    <body style="text-align:center">
                        windowId=%d
                        <br/>
                        <br/>
                        processId=%d (%s)
                        <br/>
                        <br/>
                        @%d,%d+%dx%d
                    </body>
                </html>
            """.strip();

    private final static OperatingSystem os = new SystemInfo().getOperatingSystem();

    private final int processId = (int) ProcessHandle.current().pid();

    private final java.util.List<Integer> parents = new ArrayList<>();

    private Long windowId = null;

    public Window(final String title, Color color, int x, int y) {

        OSProcess osProcess = os.getProcess(processId);
        while (osProcess.getParentProcessID() != 1) {
            parents.add(osProcess.getParentProcessID());
            osProcess = os.getProcess(osProcess.getParentProcessID());
        }

        final JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.getContentPane().setBackground(color);
        frame.setLocation(x, y);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setVisible(true);

        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 22));
        label.setForeground(color.equals(Color.BLUE) ? Color.WHITE : Color.BLACK);
        frame.getContentPane().add(label, BorderLayout.CENTER);

        Runnable updateLabel = () -> {
            if (windowId == null) {
                windowId = findWindow(processId, title).map(OSDesktopWindow::getWindowId).orElse(null);
            }
            String text = String.format(TEMPLATE,
                    windowId != null  ? windowId : "n/a",
                    processId,
                    parents.stream().map(String::valueOf).collect(Collectors.joining(", ")),
                    frame.getX(),
                    frame.getY(),
                    (int) frame.getSize().getWidth(),
                    (int) frame.getSize().getHeight()
            );

            label.setText(text);
        };

        Timer timer = new Timer(100, e -> {
            updateLabel.run();
            ((Timer) e.getSource()).stop();
        });
        timer.setInitialDelay(2);
        timer.start();

        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateLabel.run();
            }

            @Override
            public void componentMoved(java.awt.event.ComponentEvent e) {
                updateLabel.run();
            }
        });
    }

    private Optional<OSDesktopWindow> findWindow(long processId, String title) {
        return os.getDesktopWindows(true).stream()
                .filter(w -> w.getOwningProcessId() == processId)
                .filter(w -> w.getTitle().contains(title))
                .findFirst();
    }
}