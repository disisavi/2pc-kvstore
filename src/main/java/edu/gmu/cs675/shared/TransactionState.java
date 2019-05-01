package edu.gmu.cs675.shared;

public interface TransactionState {
    Integer START = 1;
    Integer ABORT = -1;
    Integer POLL = 3;
    Integer ACTIONCHECK = 4;
    Integer COMMIT = 5;
    Integer COMPLETE = 9;
}