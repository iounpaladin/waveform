package com.steinbergfour.max;

import com.steinbergfour.max.data.Production;
import com.steinbergfour.max.data.ProductionException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            Production p = Production.fromXML(new File("data/sample.xml"));
            System.out.println(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void test() {
        int channel = 9; // 0 is a piano, 9 is percussion, other channels are for other instruments

        int volume = 80; // between 0 et 127
        int duration = 200; // in milliseconds

        try {
            Synthesizer synth = MidiSystem.getSynthesizer();
            synth.open();
            MidiChannel[] channels = synth.getChannels();

            // --------------------------------------
            // Play a few notes.
            // The two arguments to the noteOn() method are:
            // "MIDI note number" (pitch of the note),
            // and "velocity" (i.e., volume, or intensity).
            // Each of these arguments is between 0 and 127.
            channels[channel].noteOn(60, volume); // C note
            Thread.sleep(duration);
            channels[channel].noteOff(60);
            channels[channel].noteOn(62, volume); // D note
            Thread.sleep(duration);
            channels[channel].noteOff(62);
            channels[channel].noteOn(64, volume); // E note
            Thread.sleep(duration);
            channels[channel].noteOff(64);

            Thread.sleep(500);

            // --------------------------------------
            // Play a C major chord.
            channels[channel].noteOn(60, volume); // C
            channels[channel].noteOn(64, volume); // E
            channels[channel].noteOn(67, volume); // G
            Thread.sleep(3000);
            channels[channel].allNotesOff();
            Thread.sleep(500);


            synth.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}