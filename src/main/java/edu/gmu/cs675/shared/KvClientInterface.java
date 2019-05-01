package edu.gmu.cs675.shared;

import javassist.NotFoundException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.TimeoutException;


// Interface to master for client
public interface KvClientInterface extends Remote {
    int port = 1024;

    void put(String key, String value) throws RemoteException, TimeoutException, IllegalStateException;

    void delete(String key) throws RemoteException, TimeoutException;

    String get(String key) throws RemoteException, NotFoundException;
}
