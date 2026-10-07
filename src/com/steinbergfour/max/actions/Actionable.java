package com.steinbergfour.max.actions;

import org.jetbrains.annotations.Nullable;

public interface Actionable {
    public void activate(@Nullable Responder responder);
}
