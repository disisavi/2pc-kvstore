package edu.gmu.cs675.server.model;

import java.io.Serializable;

public class keyValuePersistence implements Serializable {
    private String key;
    private String value;

    public keyValuePersistence(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public keyValuePersistence() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

//    TODO
//        1. Make a way to implement arralylist of value
}
