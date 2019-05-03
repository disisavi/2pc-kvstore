package edu.gmu.cs675.replica;


import edu.gmu.cs675.doa.DOA;
import edu.gmu.cs675.replica.model.KeyValuePersistence;
import edu.gmu.cs675.replica.model.TransactionLoggerReplica;
import edu.gmu.cs675.shared.KvMasterReplicaInterface;
import edu.gmu.cs675.shared.KvReplicaInterface;
import edu.gmu.cs675.shared.TransactionState;
import javassist.NotFoundException;
import org.apache.log4j.Logger;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import static java.rmi.registry.LocateRegistry.getRegistry;


public class Replica implements KvReplicaInterface {
    Logger logger = Logger.getLogger(Replica.class);
    DOA dataObject;
    Map<String, KvClass> keyValueMap;
    Map<Integer, TransactionLoggerReplica> transactionLoggerMap;
    KvMasterReplicaInterface masterApi;
    KvReplicaInterface selfStub;

    Replica() throws RemoteException, NotBoundException {
        dataObject = DOA.getDoa();
        keyValueMap = new ConcurrentHashMap<>();
        transactionLoggerMap = new ConcurrentHashMap<>();
        masterApi = this.getStub();

    }

    void persistInit() {
        HashMap<String, String> initStream = null;
        try {
            initStream = masterApi.registerReplica(this.selfStub);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (initStream.size() > 0) {
            for (Map.Entry<String, String> entry : initStream.entrySet()) {
                KeyValuePersistence keyValuePersistence = new KeyValuePersistence(entry.getKey(), entry.getValue());
                KvClass kvClass = new KvClass(entry.getKey());
                kvClass.keyValuePersistence = keyValuePersistence;
                dataObject.persistNewObject(keyValuePersistence);
            }
            dataObject.commit();
        }

    }

    KvMasterReplicaInterface getStub() throws RemoteException, NotBoundException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the Master server ip ");
        String host = scanner.next();

        Registry gameRegistry = getRegistry(host, KvMasterReplicaInterface.port);
        KvMasterReplicaInterface kvClientInterface = (KvMasterReplicaInterface) gameRegistry.lookup(KvMasterReplicaInterface.name);
        return kvClientInterface;
    }

    void shutdown() {
        this.dataObject.shutdown();
    }

    @Override
    public void delete(Integer transactionID, String key) throws RemoteException, NotFoundException {

    }

    @Override
    public String get(String key) throws RemoteException, NotFoundException {
        return null;
    }

    @Override
    public HashMap<String, String> getAll() throws RemoteException {
        return null;
    }

    @Override
    public Boolean readyToCommit(Integer transactionId, String key, String value) {
        if (transactionLoggerMap.containsKey(transactionId)) {

            transactionLoggerMap.get(transactionId).setState(TransactionState.START);
            transactionLoggerMap.get(transactionId).setKey(key);
            transactionLoggerMap.get(transactionId).setValue(value);
            dataObject.updateObject(transactionLoggerMap.get(transactionId));
        }

        TransactionLoggerReplica transactionLoggerReplica = new TransactionLoggerReplica(transactionId);
        transactionLoggerReplica.setValue(value);
        transactionLoggerReplica.setKey(key);
        dataObject.persistNewObject(transactionLoggerReplica);
        transactionLoggerMap.put(transactionId, transactionLoggerReplica);
        return true;
    }

    @Override
    public void put(Integer transactionID, String key, String value) throws RemoteException, NotFoundException {
        transactionLoggerMap.get(transactionID).setKey(key);
        transactionLoggerMap.get(transactionID).setState(TransactionState.COMMIT);
        dataObject.updateObject(transactionLoggerMap.get(transactionID));
        synchronized (this) {
            if (keyValueMap.containsKey(key)) {
                keyValueMap.get(key).dirty = true;
                keyValueMap.get(key).keyValuePersistence.setKey(key);
                keyValueMap.get(key).keyValuePersistence.setValue(value);
                dataObject.updateObject(keyValueMap.get(key).keyValuePersistence);
            } else {
                KvClass kvClass = new KvClass(key);
                KeyValuePersistence keyValuePersistence = new KeyValuePersistence(key, value);
                kvClass.dirty = false;
                kvClass.keyValuePersistence = keyValuePersistence;
                keyValueMap.put(key, kvClass);
                dataObject.persistNewObject(keyValuePersistence);
            }
        }
    }

    @Override
    public void commit(Integer transactionId) throws RemoteException {
        dataObject.commit();
    }

    @Override
    public void abortTransaction(Integer transactionId) throws RemoteException {

        TransactionLoggerReplica transactionLoggerReplica = transactionLoggerMap.get(transactionId);

        transactionLoggerReplica.setState(TransactionState.ABORT);
        dataObject.updateObject(transactionLoggerReplica);
    }

    class KvClass {
        String key;
        KeyValuePersistence keyValuePersistence;
        Boolean readLocked;
        Boolean writeLock;
        Boolean dirty;
        Long lockValue;
        Long transactionID;

        KvClass(String key) {
            this.key = key;
            this.readLocked = false;
            this.writeLock = false;
            this.dirty = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof KvClass)) return false;
            KvClass kvClass = (KvClass) o;
            return Objects.equals(key, kvClass.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
}
