package edu.gmu.cs675.server.model;

import java.io.Serializable;

public class TransactionLogger implements Serializable {
    private static final long serialVersionUID = 7285422351781570L;
    private Integer transactionId;
    private String Key;
    private String Value;
    private int state;
    private Long stamp;

    public Long getStamp() {
        return stamp;
    }

    public void setStamp(Long stamp) {
        this.stamp = stamp;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String value) {
        Value = value;
    }

    public String getKey() {
        return Key;
    }

    public void setKey(String key) {
        Key = key;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }
}