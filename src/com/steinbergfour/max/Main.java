package com.steinbergfour.max;

import com.steinbergfour.max.data.Cue;
import com.steinbergfour.max.data.Production;
import com.steinbergfour.max.triggers.KeyTriggers;
import net.miginfocom.swing.MigLayout;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
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

/**
 * @see http://stackoverflow.com/a/17767350/230513
 */
public class Main {
    private static final Random R = new Random();
    private Production p;
    private ProductionFrame frame;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Main()::display);
    }

    private Main() {
        try {
            this.p = Production.fromXML(new File("data/sample.xml"));
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
        frame.setFocusable(true);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        this.frame = frame;
    }

    public void receiveKey(Cue e) {
        this.frame.cueTriggered(e);
    } // todo: cue ends trigger

    private static class ProductionFrame extends JFrame {
        private final Production production;
        private JScrollPane cueScroller;
        private JTable cueContainer;
        private JProgressBar playBar;
        private JButton pausePlay;
        private JPanel fftPanel;
        private int selectedRow = -1;
        public static ProductionFrame singleton;

        public ProductionFrame(Production p) {
            this.production = p;
            singleton = this;

            this.initialiseComponents();
        }

        public void cueTriggered(Cue c) {
            for (int row = 0; row < this.cueContainer.getModel().getRowCount(); row++) {
                if (this.cueContainer.getModel().getValueAt(row, 0).equals(c.name())) {
//                    this.cueContainer.changeSelection(row, 0, false, false);
//                    this.cueContainer.changeSelection(row, 2, false, true);
//                    break;
                    this.selectedRow = row;
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
            this.cueContainer = new JTable(){
                public Component prepareRenderer(
                        TableCellRenderer renderer, int row,
                        int column) {
                    Component c = super.prepareRenderer(renderer,
                            row, column);
                    c.setBackground(row == ProductionFrame.singleton.selectedRow ? Color.RED : getBackground());
                    return c;
                }
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };;
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
            ){
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            });
            this.cueContainer.setRowSelectionAllowed(false);
            this.cueContainer.setColumnSelectionAllowed(false);
            this.cueContainer.setCellSelectionEnabled(false);

            // progress bar and play/pause
            this.playBar = new JProgressBar(0, (int) (targetFormat.getSampleRate() * 60));
            this.pausePlay = new JButton("\u23f8"); // \u25b6
            this.pausePlay.addActionListener(e -> {
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
