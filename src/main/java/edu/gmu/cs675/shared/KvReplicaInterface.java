package edu.gmu.cs675.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface KvReplicaInterface extends Remote {
    int port = 1025;

    Boolean voteCommitKey(String Key) throws RemoteException;

    Boolean commitInClient(String Key) throws RemoteException;
}
