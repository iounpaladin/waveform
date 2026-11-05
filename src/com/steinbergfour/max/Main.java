package com.steinbergfour.max;

import com.steinbergfour.max.data.Cue;
import com.steinbergfour.max.data.Production;
import com.steinbergfour.max.managers.CueManager;
import com.steinbergfour.max.triggers.KeyTriggers;
import net.miginfocom.swing.MigLayout;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;
import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import static com.steinbergfour.max.managers.CueManager.targetFormat;

public class Main {
    private static final Random R = new Random();
    private Production p;
    private ProductionFrame frame;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Main()::display);
    }

    private Main() {
        try {
            this.p = Production.fromXML(new File("example/maan.xml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void display() {
        if (this.p == null) {
            throw new RuntimeException();
        }
        ProductionFrame frame = new ProductionFrame(this.p);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addKeyListener(new KeyTriggers(p, this::receiveKey));
        CueManager.singleton.registerListener(
                frame::cueTriggered,
                CueManager.ListenerType.ACTIVATED
        );
        CueManager.singleton.registerListener(
                frame::cueFinished,
                CueManager.ListenerType.FINISHED
        );
        frame.setFocusable(true);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        this.frame = frame;
    }

    public void receiveKey(Cue e) {
//        this.frame.cueTriggered(e);
        // deprecated
    }

    private static class ProductionFrame extends JFrame {
        private final Production production;
        private JScrollPane cueScroller;
        private JTable cueContainer;
        private JProgressBar playBar;
        private JButton pausePlay;
        private JPanel fftPanel;
        private Vector<Integer> nowPlaying;
        public static ProductionFrame singleton;
        private Vector<Long> recentlyStarted;

        public ProductionFrame(Production p) {
            this.production = p;
            singleton = this;

            this.initialiseComponents();
        }

        public void cueTriggered(Cue c) {
            for (int row = 0; row < this.cueContainer.getModel().getRowCount(); row++) {
                if (this.cueContainer.getModel().getValueAt(row, 0).equals(c.name())) {
                    this.nowPlaying.set(
                            row,
                            this.nowPlaying.get(row) + 1
                    );
                    this.recentlyStarted.set(row, System.currentTimeMillis());
                    this.cueContainer.updateUI();
                    break;
                }
            }
        }

        public void cueFinished(Cue c) {
            for (int row = 0; row < this.cueContainer.getModel().getRowCount(); row++) {
                if (this.cueContainer.getModel().getValueAt(row, 0).equals(c.name())) {
                    this.nowPlaying.set(
                            row,
                            this.nowPlaying.get(row) - 1
                    );
                    this.cueContainer.updateUI();
                    break;
                }
            }
        }

        private void initialiseComponents() {
            // this
            this.setTitle(String.format("Production %s", this.production.getName()));
            this.getContentPane().setLayout(new MigLayout(
                    "hidemode 3",
                    // columns
                    "[fill]" +
                            "[fill]",
                    // rows
                    "[]" +
                            "[]" +
                            "[]"));


            // cue table
            this.cueScroller = new JScrollPane();
            this.cueContainer = new JTable() { // todo: flashing animation when an action cue is triggered
                public Component prepareRenderer(
                        TableCellRenderer renderer, int row,
                        int column) {
                    Component c = super.prepareRenderer(renderer,
                            row, column);
                    if (CueManager.singleton.playingBlockingCue()) {
                        var cue = CueManager.singleton.getCurrentBlockingCue();
                        c.setBackground(colorLerp(getBackground(), Color.red,  (this.getModel().getValueAt(row, 0).equals(cue.name())) ? 1 : 0));
                    }
                    return c;
                }

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            ;
            this.cueScroller.setViewportView(this.cueContainer);
            this.getContentPane().add(this.cueScroller, "cell 0 0");

            Vector<Vector<String>> data = new Vector<>();
            for (
                    var cueName :
                    this.production
                            .getCueList()
                            .keySet()
                            .stream()
                            .sorted(Comparator.naturalOrder())
                            .toList()
            ) {
                var cue = this.production.getCueList().get(cueName);
                Vector<String> row = new Vector<>();
                row.add(cue.name());
                row.add(cue.hotkey());
                row.add(cue.reminder());
                data.add(row);
            }
            this.cueContainer.setModel(new DefaultTableModel(
                    data, new Vector<>(List.of(new String[]{"Name", "Hotkey", "Reminder"}))
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            });
            this.nowPlaying = new Vector<>(Collections.nCopies(data.size(), 0));
            this.recentlyStarted = new Vector<>(Collections.nCopies(data.size(), 0L));
            this.cueContainer.setRowSelectionAllowed(false);
            this.cueContainer.setColumnSelectionAllowed(false);
            this.cueContainer.setCellSelectionEnabled(false);

            // progress bar and play/pause
            this.playBar = new JProgressBar(0, 1000);
            this.playBar.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int mouseX = e.getX();
                    int barWidth = playBar.getWidth();
                    int min = playBar.getMinimum();
                    int max = playBar.getMaximum();

                    // Calculate the new value based on click position
                    int newValue = (int) (((double) mouseX / barWidth) * (max - min) + min);

                    // Ensure value stays within bounds
                    newValue = Math.max(min, Math.min(max, newValue));

                    playBar.setValue(newValue);
                    // doesnt work
                    CueManager.singleton.seek(CueManager.singleton.getCurrentBlockingCue().name(), newValue / 1000f);
                }
            });

            Timer t = new Timer();
            t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    ProductionFrame.singleton.updateBar();
                }
            }, 0, 100);
            this.pausePlay = new JButton("⏸"); // \u25b6
            this.pausePlay.addActionListener(e -> {
                CueManager.singleton.pause();
                pausePlay.setText(CueManager.singleton.isPaused() ? "▶" : "⏸");
            });
            this.getContentPane().add(this.playBar, "cell 0 2");
            this.getContentPane().add(this.pausePlay, "cell 1 2");

            // fft panel
            this.fftPanel = new DrawPanel();
            this.getContentPane().add(this.fftPanel, "cell 1 0");

            // pack
            this.cueContainer.setFocusable(false);
            this.cueScroller.setFocusable(false);
            this.playBar.setFocusable(false);
            this.pausePlay.setFocusable(false);
            this.fftPanel.setFocusable(false);
            this.pack();
            this.setLocationRelativeTo(this.getOwner());
        }

        private void updateBar() {
            var c = CueManager.singleton;
            long now = System.currentTimeMillis();

            if (c.playingBlockingCue()) { // change this to just get microsecond position from clip
                if (c.isPaused()) {
                    System.out.println(c.getFramePausedAt());
                }
                long start = c.blockingCueStarted();
                long elapsed = c.isPaused() ? c.getFramePausedAt() : now - start;
                int thousands = (int)((elapsed / (double) c.blockingCueDuration()) * 1000);
//                long pos = this.production.getBlockingClip().getMicrosecondPosition();
//                long len = this.production.getBlockingClip().getMicrosecondLength();
//                int thousands = (int) ((pos / (double) len) * 1000);
                this.playBar.setValue(thousands);
            } else {
                this.playBar.setValue(0);
            }

            SwingUtilities.invokeLater(this.playBar::updateUI);
//            this.playBar.updateUI();
        }

        private Color colorLerp(Color a, Color b, double v) {
            v = Math.clamp(v, 0, 1);
            return new Color(
                    (int) (a.getRed() * (1 - v) + b.getRed() * v),
                    (int) (a.getGreen() * (1 - v) + b.getGreen() * v),
                    (int) (a.getBlue() * (1 - v) + b.getBlue() * v)
            );
        }
    }

    private static class DrawPanel extends JPanel implements ControllerEventListener {

        private final Font font = this.getFont().deriveFont(24f);
        private int data;

        @Override
        public void paintComponent(Graphics g) {
            g.setColor(Color.getHSBColor(R.nextFloat(), 1, 1));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(font);
            g.setColor(Color.black);
            g.drawString(String.valueOf(data), 8, g.getFontMetrics().getHeight());
        }

        @Override
        public void controlChange(ShortMessage event) {
            data = event.getData2();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(256, 128);
        }
    }
}
