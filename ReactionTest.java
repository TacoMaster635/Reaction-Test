import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Locale;
import javax.swing.*;

public class ReactionTest {

    static final Color BACKGROUND_COLOR = new Color(15, 23, 42);
    static final Color TEXT_COLOR = new Color(226, 232, 240);
    static final Color MUTED_TEXT_COLOR = new Color(148, 163, 184);
    static final Color READY_COLOR = new Color(37, 99, 235);
    static final Color WAITING_COLOR = new Color(217, 119, 6);
    static final Color CLICK_COLOR = new Color(22, 163, 74);
    static final Color RESULT_COLOR = new Color(79, 70, 229);
    static final Color ERROR_COLOR = new Color(220, 38, 38);
    static final long MAX_VALID_REACTION_TIME = 2000;
    static long startTime;
    static long bestTime = Long.MAX_VALUE;

    static String state = "READY";
    // READY, WAITING, CLICK, RESULT, RESETTING

    static JButton button;
    static JButton statsButton;
    static Timer reactionTimer;
    static CardLayout cardLayout;
    static JPanel cards;
    static JLabel attemptsValue;
    static JLabel tooSoonValue;
    static JLabel validAttemptsValue;
    static JLabel averageTimeValue;
    static boolean showingStats;

    public static int attempts = 0;
    public static int tooSoons = 0;
    public static int validAttempts = 0;
    public static long totalTime = 0;

    // For custom colors etc
    public static String customKey = "notChosen";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Reaction Test");
            frame.getContentPane().setBackground(BACKGROUND_COLOR);

            button = new JButton("START REACTION TEST");
            button.setFont(new Font("Arial", Font.BOLD, 28));
            button.setForeground(Color.WHITE);
            button.setBackground(READY_COLOR);
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(36, 48, 36, 48));
            button.setFocusPainted(false);
            button.setFocusable(false);

            statsButton = new JButton("SHOW STATS");
            statsButton.setFont(new Font("Arial", Font.BOLD, 16));
            statsButton.setForeground(TEXT_COLOR);
            statsButton.setBackground(new Color(30, 41, 59));
            statsButton.setOpaque(true);
            statsButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(51, 65, 85)),
                    BorderFactory.createEmptyBorder(12, 24, 12, 24)
            ));
            statsButton.setFocusPainted(false);
            statsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            statsButton.addActionListener(e -> statScreen());

            setReadyScreen();

            button.addActionListener(e -> handleInput());

            JRootPane rootPane = frame.getRootPane();
            KeyStroke customKeyStroke = null;
            if (customKey != null && !customKey.trim().equalsIgnoreCase("notChosen")) {
                String keyName = customKey.trim().toUpperCase(Locale.ROOT);
                if (keyName.startsWith("VK_")) {
                    keyName = keyName.substring(3);
                }
                customKeyStroke = KeyStroke.getKeyStroke("pressed " + keyName);
                if (customKeyStroke != null) {
                    rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                            .put(customKeyStroke, "reactionInput");
                }
            }
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "reactionInput");
            rootPane.getActionMap().put("reactionInput",
                    new AbstractAction() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            if (!showingStats && KeyboardFocusManager
                                    .getCurrentKeyboardFocusManager().getFocusOwner() != statsButton) {
                                handleInput();
                            }
                        }
                    });

            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            JLabel title = new JLabel("REACTION TEST");
            title.setFont(new Font("Arial", Font.BOLD, 30));
            title.setForeground(TEXT_COLOR);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel subtitle = new JLabel("How fast are your reflexes?");
            subtitle.setFont(new Font("Arial", Font.PLAIN, 16));
            subtitle.setForeground(MUTED_TEXT_COLOR);
            subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            header.add(title);
            header.add(Box.createVerticalStrut(8));
            header.add(subtitle);

            JPanel gameArea = new JPanel(new GridBagLayout());
            gameArea.setOpaque(false);
            button.setPreferredSize(new Dimension(820, 360));
            gameArea.add(button);

            JPanel footer = new JPanel();
            footer.setOpaque(false);
            footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
            statsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel hint = new JLabel("You can also use " + activationKeys());
            hint.setFont(new Font("Arial", Font.PLAIN, 13));
            hint.setForeground(MUTED_TEXT_COLOR);
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);
            footer.add(statsButton);
            footer.add(Box.createVerticalStrut(12));
            footer.add(hint);

            JPanel statsArea = createStatsArea();
            cards = new JPanel(cardLayout = new CardLayout());
            cards.setOpaque(false);
            cards.add(gameArea, "game");
            cards.add(statsArea, "stats");

            JPanel panel = new JPanel(new BorderLayout(0, 30));
            panel.setBackground(BACKGROUND_COLOR);
            panel.setBorder(BorderFactory.createEmptyBorder(36, 48, 30, 48));
            panel.add(header, BorderLayout.NORTH);
            panel.add(cards, BorderLayout.CENTER);
            panel.add(footer, BorderLayout.SOUTH);
            frame.add(panel);

            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            if (customKey != null && !customKey.trim().equalsIgnoreCase("notChosen")
                    && customKeyStroke == null) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Could not recognize customKey \"" + customKey
                                + "\". Use a key name such as A, SPACE, or F1.",
                        "Invalid custom key",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    static String activationKeys() {
        return customKey == null || customKey.trim().equalsIgnoreCase("notChosen")
                ? "ENTER"
                : "ENTER or " + customKey.trim().toUpperCase(Locale.ROOT);
    }

    static void handleInput() {

        // Start a new round
        if (state.equals("READY") || state.equals("RESULT")) {

            state = "WAITING";

            setGameButton("WAIT FOR GREEN", "Stay focused...", WAITING_COLOR);

            int delay = (int) (Math.random() * 3000) + 2000;

            reactionTimer = new Timer(delay, e -> {

                state = "CLICK";

                startTime = System.currentTimeMillis();

                setGameButton("CLICK NOW!", "Click or press " + activationKeys() + " as fast as you can", CLICK_COLOR);
            });

            reactionTimer.setRepeats(false);
            reactionTimer.start();
        }

        // User clicked correctly
        else if (state.equals("CLICK")) {

            long reactionTime =
                    System.currentTimeMillis() - startTime;

            attempts++;
            if (reactionTime <= MAX_VALID_REACTION_TIME) {
                validAttempts++;
                totalTime += reactionTime;
            }

            if (reactionTime <= MAX_VALID_REACTION_TIME && reactionTime < bestTime) {
                bestTime = reactionTime;
            }

            String result = reactionTime > MAX_VALID_REACTION_TIME
                    ? "TOO SLOW!  " + reactionTime + " ms"
                    : reactionTime + " ms";
            String best = bestTime == Long.MAX_VALUE ? "No valid score yet" : "BEST: " + bestTime + " ms";
            setGameButton(result, best + "  |  Click to play again", RESULT_COLOR);

            state = "RESULT";
        }

        // User clicked too early
        else if (state.equals("WAITING")) {

            tooSoons++;
            if (reactionTimer != null) {
                reactionTimer.stop();
            }

            state = "RESETTING";

            setGameButton("TOO SOON!", "Wait for green before clicking", ERROR_COLOR);

            Timer reset = new Timer(1500, e -> setReadyScreen());

            reset.setRepeats(false);
            reset.start();
        }
    }

    public static void setReadyScreen() {

        state = "READY";

        if (bestTime == Long.MAX_VALUE) {
            setGameButton("START REACTION TEST", "Click or press " + activationKeys() + " to begin", READY_COLOR);
        } else {
            setGameButton("START REACTION TEST", "BEST: " + bestTime + " ms  |  Click to play", READY_COLOR);
        }
    }

    static void setGameButton(String title, String subtitle, Color color) {
        button.setText("<html><center><font size='7'>" + title
                + "</font><br><br><font size='4'>" + subtitle
                + "</font></center></html>");
        button.setBackground(color);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static JPanel createStatsArea() {
        JPanel statsArea = new JPanel();
        statsArea.setOpaque(false);
        statsArea.setLayout(new BoxLayout(statsArea, BoxLayout.Y_AXIS));
        statsArea.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));

        JLabel heading = new JLabel("YOUR STATS");
        heading.setFont(new Font("Arial", Font.BOLD, 30));
        heading.setForeground(TEXT_COLOR);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsArea.add(heading);
        statsArea.add(Box.createVerticalStrut(28));

        attemptsValue = addStatRow(statsArea, "ATTEMPTS", "0");
        tooSoonValue = addStatRow(statsArea, "TOO SOON", "0");
        validAttemptsValue = addStatRow(statsArea, "VALID ATTEMPTS", "0");
        averageTimeValue = addStatRow(statsArea, "AVERAGE REACTION", "No valid attempts yet");
        return statsArea;
    }

    static JLabel addStatRow(JPanel panel, String label, String value) {
        JLabel row = new JLabel(label + "     " + value);
        row.setFont(new Font("Arial", Font.PLAIN, 20));
        row.setForeground(TEXT_COLOR);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(row);
        panel.add(Box.createVerticalStrut(16));
        return row;
    }

    static void statScreen() {
        showingStats = !showingStats;
        if (showingStats) {
            String averageTime = validAttempts > 0
                    ? (totalTime / validAttempts) + " ms"
                    : "No valid attempts yet";
            attemptsValue.setText("ATTEMPTS     " + attempts);
            tooSoonValue.setText("TOO SOON     " + tooSoons);
            validAttemptsValue.setText("VALID ATTEMPTS     " + validAttempts);
            averageTimeValue.setText("AVERAGE REACTION     " + averageTime);
            cardLayout.show(cards, "stats");
            statsButton.setText("BACK TO GAME");
        } else {
            cardLayout.show(cards, "game");
            statsButton.setText("SHOW STATS");
        }
    }

}