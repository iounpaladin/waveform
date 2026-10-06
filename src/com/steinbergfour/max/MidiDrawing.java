package com.steinbergfour.max;

import com.steinbergfour.max.data.Production;
import com.steinbergfour.max.data.ProductionException;
import com.steinbergfour.max.triggers.KeyTriggers;
import org.xml.sax.SAXException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @see http://stackoverflow.com/a/17767350/230513
 */
public class MidiDrawing {

    private static final Random R = new Random();
    private Production p;

    public static void main(String[] args) {
        EventQueue.invokeLater(new MidiDrawing()::display);
    }

    private MidiDrawing() {
        try {
            this.p = Production.fromXML(new File("data/sample.xml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void display() {
        JFrame frame = new JFrame("Midi Drawing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addKeyListener(new KeyTriggers(p));
        frame.setFocusable(true);
        DrawPanel dp = new DrawPanel();
        frame.add(dp);
        dp.setFocusable(true);
        Sequencer sequencer = initSequencer(dp);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.add(new JButton(new AbstractAction("Start") {
            @Override
            public void actionPerformed(ActionEvent e) {
                sequencer.setTickPosition(0);
                sequencer.start();
            }
        }));
        frame.add(p, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private Sequencer initSequencer(DrawPanel dp) {
        try {
            Sequencer sequencer = MidiSystem.getSequencer();
            Sequence seq = new Sequence(Sequence.PPQ, 3);
            Track track = seq.createTrack();
            int n = 60; // middle C
            for (int i = 0; i < 3 * 12; i += 3) {
                track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 0, n), i));
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, n, 127), i));
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, n + 3, 127), i));
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, n, 127), i + 3));
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, n + 3, 127), i + 3));
                n++;
            }
            sequencer.open();
            sequencer.setSequence(seq);
            sequencer.addControllerEventListener(dp, new int[]{0});
            return sequencer;
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            e.printStackTrace(System.err);
        }
        return null;
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
