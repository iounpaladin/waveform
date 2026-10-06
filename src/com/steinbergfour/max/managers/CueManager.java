package com.steinbergfour.max.managers;

import com.steinbergfour.max.data.Cue;
import com.steinbergfour.max.data.CueList;
import com.steinbergfour.max.data.PostprocessingList;
import com.steinbergfour.max.data.ResourceList;

public class CueManager {
    public CueManager(CueList cl, ResourceList rl, PostprocessingList ppl) {

    }

    public void preloadResources() {

    }

    public void trigger(Cue c) {
        System.out.println(c);
    }
}
