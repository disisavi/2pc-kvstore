package edu.gmu.cs675.replica.model;

import edu.gmu.cs675.shared.TransactionState;

import java.io.Serializable;
import java.util.Objects;

public class TransactionLoggerReplica implements Serializable {
    private static final long serialVersionUID = 7285422351781570L;
    private Integer seq;
    private Integer transactionId;
    private String Key;
    private String Value;
    private int state;



    public TransactionLoggerReplica(Integer transactionId) {
        this.transactionId = transactionId;
        this.state = TransactionState.START;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionLoggerReplica)) return false;
        TransactionLoggerReplica that = (TransactionLoggerReplica) o;
        return getTransactionId().equals(that.getTransactionId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTransactionId());
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }
}
