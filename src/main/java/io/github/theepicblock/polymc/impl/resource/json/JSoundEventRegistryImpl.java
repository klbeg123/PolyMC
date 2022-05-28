package io.github.theepicblock.polymc.impl.resource.json;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.github.theepicblock.polymc.api.resource.json.JSoundEvent;
import io.github.theepicblock.polymc.api.resource.json.JSoundEventRegistry;
import io.github.theepicblock.polymc.impl.Util;
import io.github.theepicblock.polymc.impl.resource.ResourceGenerationException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public class JSoundEventRegistryImpl implements JSoundEventRegistry {
    private static final Type TYPE = new TypeToken<Map<String,JSoundEventImpl>>() {}.getType();
    private Map<String, JSoundEvent> jsonRepresentation;

    public JSoundEventRegistryImpl() {
        this.jsonRepresentation = new HashMap<>();
    }

    public JSoundEventRegistryImpl(HashMap<String,JSoundEvent> jsonRepresentation) {
        this.jsonRepresentation = jsonRepresentation;
    }

    public static JSoundEventRegistryImpl of(InputStream inputStream, @Nullable String name) {
        try {
            var jsonReader = new JsonReader(new InputStreamReader(inputStream));
            jsonReader.setLenient(true);

            return new JSoundEventRegistryImpl(Util.GSON.fromJson(jsonReader, TYPE));
        } catch (JsonSyntaxException e) {
            throw new ResourceGenerationException("Error reading sound event registry for "+name, e);
        }
    }

    @Override
    public Map<String,JSoundEvent> getMap() {
        return jsonRepresentation;
    }

    @Override
    public void write(Path location, Gson gson) throws IOException {
        var writer = new FileWriter(location.toFile(), StandardCharsets.UTF_8);
        gson.toJson(jsonRepresentation, writer);
        writer.close();
    }
}