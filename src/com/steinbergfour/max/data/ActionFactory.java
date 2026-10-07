package com.steinbergfour.max.data;

import com.steinbergfour.max.actions.Actionable;
import com.steinbergfour.max.actions.KillAction;
import com.steinbergfour.max.actions.PlayAction;
import org.w3c.dom.Node;

public class ActionFactory {
    public static enum ActionType {
        PLAY, KILL, MIDI
    }
    public static Actionable fromXML(Node action) {
        String type, target, preset = "";

        var attr = action.getAttributes();
        type = attr.getNamedItem("type").getNodeValue();
        ActionType ty = ActionType.valueOf(type.toUpperCase());
        target = attr.getNamedItem("target").getNodeValue();
        if (attr.getNamedItem("preset") != null) {
            preset = attr.getNamedItem("preset").getNodeValue();
        }

        return switch(ty) {
            case PLAY -> new PlayAction(target, preset);
            case KILL -> new KillAction(target);
            default -> null;
        };
    }
}
