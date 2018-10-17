package com.neromatt.epiphany.model.Gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.DataObjects.SingleRack;

import java.lang.reflect.Type;

public class ModelTypeAdapter implements JsonSerializer<MainModel>, JsonDeserializer<MainModel> {

    @Override
    public JsonElement serialize(MainModel src, Type typeOfSrc, JsonSerializationContext context) {
        return src.toJson();
    }

    @Override
    public MainModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        int type = json.getAsJsonObject().get("modelType").getAsInt();
        switch (type) {
            case MainModel.TYPE_RACK:
                return new SingleRack(json.getAsJsonObject());
            case MainModel.TYPE_FOLDER:
                return new SingleNotebook(json.getAsJsonObject());
            case MainModel.TYPE_MARKDOWN_NOTE:
                return new SingleNote(json.getAsJsonObject());
        }
        return null;
    }
}
