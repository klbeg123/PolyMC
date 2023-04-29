package io.github.theepicblock.polymc.impl.generator.asm.stack;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public record KnownInteger(int i) implements StackEntry {
    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(i);
    }

    @Override
    public <T> T cast(Class<T> type) {
        if (type == Integer.class) {
            return (T)(Integer)i;
        }
        return StackEntry.super.cast(type);
    }
}