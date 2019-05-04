package edu.gmu.cs675.master.model;

import edu.gmu.cs675.shared.TransactionState;

import java.io.Serializable;
import java.util.Objects;

public class TransactionLogger implements Serializable {
    private static final long serialVersionUID = 7285422351781570L;
    private Integer transactionId;
    private String key;
    private String Value;
    private int state;
    private String operation;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }


    public TransactionLogger(Integer transactionId) {
        this.transactionId = transactionId;
        this.state = TransactionState.START;
    }

    public TransactionLogger() {
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
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionLogger)) return false;
        TransactionLogger that = (TransactionLogger) o;
        return getTransactionId().equals(that.getTransactionId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTransactionId());
    }

}
