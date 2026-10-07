package com.steinbergfour.max.data;

import com.steinbergfour.max.managers.CueManager;
import com.steinbergfour.max.managers.SoundManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class Production {
    private String name;
    private ResourceList resources;
    private CueList cues;
    private PostprocessingList ppl;
    private CueManager cueManager;
    private boolean initialisedCueManager;
    // midi list tbd

    private Production(String name) {
        this.name = name;
        this.cues = new CueList();
        this.resources = new ResourceList();
        this.initialisedCueManager = false;
    }

    public void initialiseCueManager() throws ProductionException {
        if (this.initialisedCueManager) {
            throw new ProductionException("");
        }
        this.cueManager = new CueManager(this.cues, this.resources, this.ppl);
        this.initialisedCueManager = true;
    }

    public static Production fromXML(File xml) throws ParserConfigurationException, IOException, SAXException, ProductionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(xml);

        var production = document.getDocumentElement();
        assert production.getNodeName().equals("production");

        var meta = production.getElementsByTagName("meta").item(0);
        var data = production.getElementsByTagName("data").item(0);

        // Meta
        if (meta == null) {
            throw new ProductionException("Missing meta tag");
        }

        var name = ((Element) meta).getElementsByTagName("name").item(0).getTextContent();
        Production p = new Production(name);

        // Data
        if (data == null) {
            throw new ProductionException("Missing data tag");
        }

        // == cues ==
        CueList cl = new CueList();
        var cues = ((Element) data).getElementsByTagName("cue");
        for (int idx = 0; idx < cues.getLength(); idx++) {
            var cue = cues.item(idx);
            var attr = cue.getAttributes();
            String hotkey = attr.getNamedItem("hotkey").getNodeValue();
            String id = attr.getNamedItem("id").getNodeValue();
            String cueName = attr.getNamedItem("name").getNodeValue();

            String reminder = ((Element) cue).getElementsByTagName("reminder").item(0).getTextContent();

            // == actions ==
            ActionList actionList = new ActionList();

            var red = ((Element) cue).getElementsByTagName("actions").item(0).getAttributes().getNamedItem("reduce").getNodeValue().toUpperCase();
            var actions = ((Element) cue).getElementsByTagName("action");

            for (int j = 0; j < actions.getLength(); j++) {
                var action = actions.item(j);

                actionList.add(ActionFactory.fromXML(action));
            }

            Cue c = new Cue(hotkey, cueName, reminder, actionList, Cue.Reduction.valueOf(red));

            cl.put(id, c);
        }

        p.setCueList(cl);

        // Resources
        ResourceList rl = new ResourceList();
        var resources = ((Element) data).getElementsByTagName("resource");
        for (int idx = 0; idx < resources.getLength(); idx++) {
            var resource = resources.item(idx);
            var attr = resource.getAttributes();
            var id =  attr.getNamedItem("id").getNodeValue();

            Resource r = new Resource(
                    id,
                    attr.getNamedItem("href").getNodeValue()
            );
            rl.put(id, r);
        }

        p.setResourceList(rl);

        p.initialiseCueManager();
        return p;
    }

    public CueList getCueList() {
        return this.cues;
    }

    private void setCueList(CueList cl) {
        this.cues = cl;
    }


    public ResourceList getResourceList() {
        return this.resources;
    }

    private void setResourceList(ResourceList cl) {
        this.resources = cl;
    }

    public String getName() {
        return name;
    }

    public void trigger(Cue c) {
        this.cueManager.trigger(c);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Name: ").append(name).append("\n");
        for(var cue : this.cues.keySet()) {
            stringBuilder.append("[").append(cue).append("]: ").append(this.cues.get(cue).toString()).append("\n");
        }

        return stringBuilder.toString();
    }
}