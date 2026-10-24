package com.steinbergfour.max.data;

import java.util.HashMap;

public record Processing(String id, String type, HashMap<String, String> parameters) {
}
