package com.neromatt.epiphany.model.DataObjects;

public class SingleNotebook extends MainModel {

    private String path;
    private String name;
    private int noteCount;

    public SingleNotebook(String name, String path, int noteCount){
        this.path = path;
        this.name = name;
        this.noteCount = noteCount;
        this.modelType = MainModel.TYPE_FOLDER;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    public int getNoteCount(){
        return this.noteCount;
    }

    public String getNoteCountAsString(){
        if (this.noteCount > 0) {
            return "" + this.noteCount;
        }
        return "";
    }
}
