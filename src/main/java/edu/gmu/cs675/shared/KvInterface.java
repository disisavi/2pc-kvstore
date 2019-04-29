package edu.gmu.cs675.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface KvInterface extends Remote {
    int port = 1024;

    void put(String key, String value) throws RemoteException;

    void delete(String key) throws RemoteException;

    String get(String key) throws RemoteException;

}