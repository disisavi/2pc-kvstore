package edu.gmu.cs675.replica;


import edu.gmu.cs675.doa.DOA;
import edu.gmu.cs675.replica.model.Cordinators;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.rmi.registry.LocateRegistry.getRegistry;


public class Replica implements KvReplicaInterface {
    Logger logger = Logger.getLogger(Replica.class);
    Map<String, KvClass> keyValueMap;
    Map<Integer, TransactionLoggerReplica> transactionLoggerMap;
    List<KvMasterReplicaInterface> masterList;
    KvReplicaInterface selfStub;
    DOA dataObject;

    Replica() throws RemoteException, NotBoundException {
        keyValueMap = new ConcurrentHashMap<>();
        transactionLoggerMap = new ConcurrentHashMap<>();
        dataObject = DOA.getDoa();
        masterList = this.getStubsList();

    }

    /**
     * @implNote First, we check if the DB already had a few Key value values. And if it did, we will delete it so that itoesnt mess with the more up to date values later.
     * Then, we will get the coordinator information from DB. If the replica is being setup for the first time, there wont be any value. In any case. if we are
     * not able to contact coordinators in DB (maybe thecoordinatorr migrated), the user will then have option to enter ip for the new master
     * We will, at this point, get the stream of most uptodate keys and values from the Server and persist those values.
     */
    void replicaStartup() throws RemoteException {
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "1000");

        Set<Object> keyValuePersistenceSet = dataObject.getAll(KeyValuePersistence.class);
        if (keyValuePersistenceSet.size() != 0) {
            for (Object object : keyValuePersistenceSet) {
                KeyValuePersistence keyValuePersistence = (KeyValuePersistence) object;
                dataObject.removeObject(keyValuePersistence);
            }
            dataObject.commit();
        }

        HashMap<String, String> initStream;
        try {
            initStream = masterList.get(0).registerReplica(this.selfStub);
        } catch (RemoteException e) {
            logger.error("Unable to get init stream");
            logger.error("stacktrace -", e);
            throw e;
        } catch (IllegalArgumentException e) {
            System.out.println("couldn't get init stream.. will try again by de registering and then registering.");
            logger.error("stacktrace ", e);
            try {
                masterList.get(0).deRegisterReplica();
                initStream = masterList.get(0).registerReplica(this.selfStub);
            } catch (RemoteException ex) {
                logger.error("Trace -- ", ex);
                throw ex;
            }

        }

        if (null != initStream && initStream.size() > 0) {
            for (Map.Entry<String, String> entry : initStream.entrySet()) {
                KeyValuePersistence keyValuePersistence = new KeyValuePersistence(entry.getKey(), entry.getValue());
                KvClass kvClass = new KvClass(entry.getKey());
                kvClass.keyValuePersistence = keyValuePersistence;
                dataObject.persistNewObject(keyValuePersistence);
            }
            dataObject.commit();
        }

    }

    List<KvMasterReplicaInterface> getStubsList() throws RemoteException {
        Set<Object> objectSet = dataObject.getAll(Cordinators.class);
        List<KvMasterReplicaInterface> replicaInterfaces = new ArrayList<>();
        try {
            if (objectSet.size() == 0) {

                replicaInterfaces.add(getStub(false, null));
            } else {
                try {
                    for (Object object : objectSet) {
                        Cordinators cordinators = (Cordinators) object;

                        replicaInterfaces.add(getStub(true, cordinators.getIP()));

                    }
                } catch (RemoteException e) {
                    logger.error("Unable to contact coordinator");
                    logger.error("Remote Exception.. ", e);
                    logger.info("Will now take user input for ip");
                    System.out.println("Couldn't connect to previously connected Coordinator...");
                    System.out.println("Lets try with a new coordinator");
                    replicaInterfaces.clear();
                    replicaInterfaces.add(getStub(false, null));
                }
            }
            return replicaInterfaces;
        } catch (RemoteException | NotBoundException e) {
            System.out.println("Unable to contact Coordinator with Given Ip");
            logger.error("Unable to contact Coordinator with Given Ip");
            logger.error("Stack trace", e);
            logger.info("Shutting Down");
            throw new RemoteException(e.getMessage());
        }
    }

    KvMasterReplicaInterface getStub(Boolean isIpGiven, String ip) throws RemoteException, NotBoundException {
        String host;
        if (isIpGiven) {
            host = ip;
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter the Master server ip ");
            host = scanner.next();
        }
        Registry gameRegistry = getRegistry(host, KvMasterReplicaInterface.port);
        KvMasterReplicaInterface kvClientInterface = (KvMasterReplicaInterface) gameRegistry.lookup(KvMasterReplicaInterface.name);
        Set<Object> objectSet = dataObject.getAll(Cordinators.class);
        for (Object object : objectSet) {
            Cordinators removeCordinator = (Cordinators) object;
            dataObject.removeObject(removeCordinator);
        }
        Cordinators cordinators = new Cordinators(host);
        dataObject.persistNewObject(cordinators);
        dataObject.commit();
        return kvClientInterface;
    }


    void shutdown() {
        this.dataObject.shutdown();
    }

    @Override
    public void delete(Integer transactionID, String key) throws RemoteException, NotFoundException {
        transactionLoggerMap.get(transactionID).setKey(key);
        transactionLoggerMap.get(transactionID).setState(TransactionState.COMMIT);
        dataObject.updateObject(transactionLoggerMap.get(transactionID));
        synchronized (this) {
            if (keyValueMap.containsKey(key)) {
                KeyValuePersistence keyValuePersistence = keyValueMap.get(key).keyValuePersistence;
                keyValueMap.remove(key);
                dataObject.removeObject(keyValuePersistence);
            } else {
                throw new NotFoundException("Key Not found");
            }
        }
    }

    @Override
    public String get(String key) throws NotFoundException {
        if (keyValueMap.containsKey(key)) {
            System.out.println("Asked for key " + key + ", value returned " + keyValueMap.get(key).keyValuePersistence.getValue());
            return keyValueMap.get(key).keyValuePersistence.getValue();
        } else throw new NotFoundException("Key not found.");
    }

    /**
     * @return HashMap<String, String> --> returns all the key value pairs in the replica.
     * @throws RemoteException
     */
    @Override
    public HashMap<String, String> getAll() throws RemoteException {
        HashMap<String, String> returnMap = new HashMap<>();
        keyValueMap.forEach(((s, kvClass) -> returnMap.put(s, kvClass.keyValuePersistence.getValue())));
        System.out.println("Asked for all kv pairs");
        return returnMap;
    }

    /**
     * @param transactionId
     * @param key
     * @param value
     * @return Boolean --> Vote true for yes, its ready to commit, or under the current implementation, throw an error if its not able to commit
     */
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
        System.out.println("\nvoted yes for commit ");
        return true;
    }


    /**
     * @param transactionID
     * @param key
     * @param value
     * @throws RemoteException
     * @throws NotFoundException The replica, in this and all the other methods, is solely dependent on Coordinator to ensure sequentially. We are, of course right now not worried about multiple coordinator.
     *                           And hence, we dont maintain any locks here.
     */
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
            System.out.println("\t{k,v} --" + key + ", " + value);
        }
    }

    @Override
    public void commit(Integer transactionId) throws RemoteException {
        transactionLoggerMap.get(transactionId).setState(TransactionState.COMPLETE);
        dataObject.updateObject(transactionLoggerMap.get(transactionId));
        dataObject.commit();
        System.out.println("\tThe transaction " + transactionId + " is committed");
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
