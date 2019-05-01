package edu.gmu.cs675.replica;

import edu.gmu.cs675.master.doa.DOA;
import edu.gmu.cs675.shared.KvReplicaInterface;
import javassist.NotFoundException;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.util.Map;


public class Replica implements KvReplicaInterface {
    Logger logger = Logger.getLogger(Replica.class);
    DOA dataObject;
    


    void shutdown(){

    }

    @Override
    public void delete(Integer transactionID, String key) throws RemoteException, NotFoundException {

    }

    @Override
    public String get(String key) throws RemoteException, NotFoundException {
        return null;
    }

    @Override
    public Map<String, String> getAll() throws RemoteException {
        return null;
    }

    @Override
    public Boolean readyToCommit(Integer transactionId, String key, String value) throws RemoteException {
        return null;
    }

    @Override
    public void put(Integer transactionID, String key, String value) throws RemoteException, NotFoundException {

    }

    @Override
    public Boolean commit(Integer transactionId) throws RemoteException {
        return null;
    }

    @Override
    public void abortCommit(Integer transactionId) throws RemoteException {

    }
}
