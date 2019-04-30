package edu.gmu.cs675.server.model;

import java.io.Serializable;
import java.util.Objects;

public class KeyValuePersistence implements Serializable {
    private static final long serialVersionUID = -249196446739301055L;
    private String key;
    private String value;

    public KeyValuePersistence(String key, String value) {
        this.key = key;
        this.value = value;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeyValuePersistence)) return false;
        KeyValuePersistence that = (KeyValuePersistence) o;
        return Objects.equals(getKey(), that.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey());
    }

//    TODO
//        1. Make a way to implement arralylist of value
}
