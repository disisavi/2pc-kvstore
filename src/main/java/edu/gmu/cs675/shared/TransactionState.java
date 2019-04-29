package edu.gmu.cs675.shared;

enum TransactionState {
    ABORT(-1),
    START(1),
    COMPLETE(9);


    private final int code;

    TransactionState(int i) {
        this.code = i;
    }

    public int getCode() {
        return code;
    }
}
