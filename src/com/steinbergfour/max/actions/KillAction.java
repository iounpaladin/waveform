package com.steinbergfour.max.actions;

public class KillAction implements Actionable {
    private final String target;

    public KillAction(String target) {
        this.target = target;
    }

    @Override
    public void activate(Responder responder) {
        System.out.println("Killing " + target);
        if(responder != null) responder.respond();
    }
}
