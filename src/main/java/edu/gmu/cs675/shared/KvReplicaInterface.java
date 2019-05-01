package edu.gmu.cs675.shared;

import javassist.NotFoundException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

//Master will talk to replica using the following API

public interface KvReplicaInterface extends Remote {
    int port = 1023;

    void delete(Integer transactionID, String key) throws RemoteException, NotFoundException;

    String get(String key) throws RemoteException, NotFoundException;

    Map<String, String> getAll() throws RemoteException;

    Boolean readyToCommit(Integer transactionId, String key, String value) throws RemoteException;

    void put(Integer transactionID, String key, String value) throws RemoteException, NotFoundException;

    Boolean commit(Integer transactionId) throws RemoteException;

    void abortCommit(Integer transactionId) throws RemoteException;
}
