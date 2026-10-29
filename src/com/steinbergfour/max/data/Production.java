package com.steinbergfour.max.data;

import com.steinbergfour.max.managers.CueManager;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class Production {
    private final String name;
    private ResourceList resources;
    private CueList cues;
    private ProcessingList processingList;
    private CompositionList compositionList;
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
        this.cueManager = new CueManager(this.cues, this.resources, this.processingList, this.compositionList);
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

            var reminderObj = ((Element) cue).getElementsByTagName("reminder").item(0);
            String reminder = reminderObj == null ? "" : reminderObj.getTextContent();
            // == actions ==
            ActionList actionList = new ActionList();

            var red = ((Element) cue).getElementsByTagName("actions").item(0).getAttributes().getNamedItem("reduce").getNodeValue().toUpperCase();
            var actions = ((Element) cue).getElementsByTagName("action");

            for (int j = 0; j < actions.getLength(); j++) {
                var action = actions.item(j);

                actionList.add(ActionFactory.fromXML(cueName, action));
            }

            Cue c = new Cue(hotkey, cueName, reminder, actionList, Cue.Reduction.valueOf(red));

            cl.put(id, c);
        }

        p.setCueList(cl);

        // Resources
        ResourceList rl = getResourceList((Element) data);

        p.setResourceList(rl);

        // Processing
        ProcessingList pl = getProcessingList((Element) data);
        p.setProcessingList(pl);

        // Compositions
        CompositionList compList = getCompositionList((Element) data);
        p.setCompositionList(compList);


        p.initialiseCueManager();
        return p;
    }

    private static @NotNull ResourceList getResourceList(Element data) {
        ResourceList rl = new ResourceList();
        var resources = data.getElementsByTagName("resource");
        for (int idx = 0; idx < resources.getLength(); idx++) {
            var resource = resources.item(idx);
            var attr = resource.getAttributes();
            var id = attr.getNamedItem("id").getNodeValue();

            Resource r = new Resource(
                    id,
                    attr.getNamedItem("href").getNodeValue()
            );
            rl.put(id, r);
        }
        return rl;
    }

    private static @NotNull ProcessingList getProcessingList(Element data) {
        ProcessingList processingList = new ProcessingList();
        var filters = ((Element) data).getElementsByTagName("filter");
        for (int idx = 0; idx < filters.getLength(); idx++) {
            var filter = filters.item(idx);
            var attr = filter.getAttributes();
            String id = attr.getNamedItem("id").getNodeValue();
            String type = attr.getNamedItem("type").getNodeValue();

            HashMap<String, String> params = new HashMap<>();

            for (int j = 0; j < attr.getLength(); j++) {
                var param = attr.item(j);
                var key = param.getNodeName();
                var value = param.getNodeValue();

                if (!key.equals("id") && !key.equals("type")) {
                    params.put(key, value);
                }
            }

            // put params

            Processing processing = new Processing(id, type, params);

            processingList.put(id, processing);
        }
        return processingList;
    }

    private static @NotNull CompositionList getCompositionList(Element data) {
        CompositionList compositionList = new CompositionList();
        var compositions = ((Element) data).getElementsByTagName("composition");
        for (int idx = 0; idx < compositions.getLength(); idx++) {
            var composition = compositions.item(idx);
            var attr = composition.getAttributes();
            var id = attr.getNamedItem("id").getNodeValue();

            var samples = ((Element) composition).getElementsByTagName("sample");
            var sampleList = new LinkedList<Sample>();

            for (int j = 0; j < samples.getLength(); j++) {
                var sample =  samples.item(j);
                String type = sample.getAttributes().getNamedItem("type").getNodeValue();
                String target = sample.getAttributes().getNamedItem("target").getNodeValue();
                String filters = sample.getAttributes().getNamedItem("filters").getNodeValue();
                Sample sampleData = new Sample(
                        type, target, filters
                );

                sampleList.add(sampleData);
            }

            Composition c = new Composition(
                    id,
                    sampleList
            );

            compositionList.put(id, c);
        }
        return compositionList;

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
        for (var cue : this.cues.keySet()) {
            stringBuilder.append("[").append(cue).append("]: ").append(this.cues.get(cue).toString()).append("\n");
        }

        return stringBuilder.toString();
    }

    private void setProcessingList(ProcessingList processingList) {
        this.processingList = processingList;
    }

    public CompositionList getCompositionList() {
        return compositionList;
    }

    private void setCompositionList(CompositionList compositionList) {
        this.compositionList = compositionList;
    }
}