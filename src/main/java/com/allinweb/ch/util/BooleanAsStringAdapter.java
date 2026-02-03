package com.allinweb.ch.util;

import com.google.gson.*;
import java.lang.reflect.Type;

public class BooleanAsStringAdapter implements JsonDeserializer<String> {
    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) {
        if (json == null || json.isJsonNull()) return null;
        if (json.isJsonPrimitive()) {
            JsonPrimitive p = json.getAsJsonPrimitive();
            if (p.isBoolean()) return String.valueOf(p.getAsBoolean());
            if (p.isString()) return p.getAsString();
            if (p.isNumber()) return p.getAsNumber().toString();
        }
        return json.toString();
    }
}
