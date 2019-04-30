package edu.gmu.cs675.shared;

import javassist.NotFoundException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface KvMasterInterface extends Remote {
    int port = 1024;
    String name = "mvMaster";

    void registerClient(KvReplicaInterface kvClinet) throws RemoteException, IllegalArgumentException;

    void deRegisterClient() throws RemoteException, IllegalArgumentException;

    Integer getTransactionID() throws RemoteException;

    Long getLockForKey(Boolean readWriteFlag, String key, Integer transactionID) throws RemoteException, NotFoundException;

    void giveLoclForKey(Boolean readWriteFlag, String key, Integer transactionID) throws RemoteException, NotFoundException;

    void put(Integer transactionID, String key, String value) throws RemoteException;

    void delete(Integer transactionID, String key) throws RemoteException;

    String get(String key) throws RemoteException, NotFoundException;

    Map<String, String> getKVStream() throws RemoteException;
}