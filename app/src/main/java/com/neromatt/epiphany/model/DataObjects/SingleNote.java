package com.neromatt.epiphany.model.DataObjects;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SingleNote extends MainModel {

    private String path;
    private String summary;
    private String name;
    private Date lastModifiedDate;
    private Date createdDate;

    public SingleNote(String path, String summary, String name, Date date){
        this.path=path;
        this.summary=summary;
        this.name=name;
        this.lastModifiedDate = date;
        this.modelType = MainModel.TYPE_MARKDOWN_NOTE;
    }

    @Override
    public String getName(){
        return this.name.substring(0,this.name.lastIndexOf('.'));
    }

    @Override
    public String getPath(){
        return path;
    }

    public String getSummary() {
        return summary;
    }

    public String getLastModifiedString(Locale loc) {
        return new SimpleDateFormat("MM/dd/yyyy", loc).format(lastModifiedDate);
    }
    public String getCreatedString(Locale loc) {
        return new SimpleDateFormat("MM/dd/yyyy", loc).format(createdDate);
    }

    public Date getLastModifiedDate(){
        return lastModifiedDate;
    }
    public Date getCreatedDate(){
        return createdDate;
    }

    public String getExtension() {return this.name.substring(this.name.toString().lastIndexOf('.') + 1);}
    public String getFullPath() {return getPath()+"/"+getName()+"."+getExtension();}
}
