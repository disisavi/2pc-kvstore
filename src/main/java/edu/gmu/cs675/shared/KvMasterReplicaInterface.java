package edu.gmu.cs675.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;


//Replica will talk to Master Using this API
public interface KvMasterReplicaInterface extends Remote {
    int port = 1025;
    String name = "mvMaster";

    HashMap<String, String> registerReplica(KvReplicaInterface kvClient) throws RemoteException, IllegalArgumentException;

    void deRegisterReplica() throws RemoteException, IllegalArgumentException;

}