package edu.gmu.cs675.master;


import edu.gmu.cs675.doa.DOA;
import edu.gmu.cs675.master.model.TransactionLogger;
import edu.gmu.cs675.shared.KvClientInterface;
import edu.gmu.cs675.shared.KvMasterReplicaInterface;
import edu.gmu.cs675.shared.KvReplicaInterface;
import edu.gmu.cs675.shared.TransactionState;
import javassist.NotFoundException;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;


//Take request from Client and contact replica regarding the request
public class KvStoreMasterClient implements KvClientInterface {
    private static Logger logger = Logger.getLogger(KvStoreMasterClient.class);
    private Map<Integer, TransactionLogger> transactionLoggerMap;
    private Map<String, StampedLock> keyLockMap;
    private DOA dataObject;

    static final AtomicInteger ongoingTransactions = new AtomicInteger(0);
    static final AtomicInteger ongoingUserChanges = new AtomicInteger(0);
    static final AtomicInteger currentTransactionID = new AtomicInteger(-1);


    KvStoreMasterClient(InetAddress ip) throws RemoteException {
        this.startRMIServer();
        this.keyLockMap = new ConcurrentHashMap<>();
        this.transactionLoggerMap = new ConcurrentHashMap<>();
        this.dataObject = DOA.getDoa();
    }


    public void startRMIServer() throws RemoteException {
        KvStoreMasterReplica kvStoreMaster = new KvStoreMasterReplica();

        try {

            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(KvMasterReplicaInterface.port);
            } catch (RemoteException e) {
                logger.info("Unable to create registry.... Checking if registry already exist");
                registry = LocateRegistry.getRegistry(KvMasterReplicaInterface.port);
            }
            KvMasterReplicaInterface nodeStub = (KvMasterReplicaInterface) UnicastRemoteObject.exportObject(kvStoreMaster, KvMasterReplicaInterface.port);

            registry.rebind(KvMasterReplicaInterface.name, nodeStub);
            System.out.println("Binding complete");
        } catch (RemoteException e) {
            System.out.println("KV Coordinator Startup Failure ... Proceeding to shutdown");
            logger.error("KV Store Startup Failure ...");
            throw e;
        }
    }

    void shutdown() {
        dataObject.shutdown();
    }

    private void changeTransactionLoggerState(Integer transactionID, Integer state, String key, String value) {
        Boolean newtransaction = false;
        TransactionLogger transactionLogger = null;
        if (this.transactionLoggerMap.containsKey(transactionID)) {
            transactionLogger = this.transactionLoggerMap.get(transactionID);
            if (!key.equals(transactionLogger.getKey())) {
                transactionLogger.setKey(key);
                transactionLogger.setValue(value);
            }

        } else {
            newtransaction = true;
            transactionLogger = new TransactionLogger(transactionID);
            transactionLogger.setKey(key);
            transactionLogger.setValue(value);
            transactionLogger.setState(state);
            transactionLoggerMap.put(transactionID, transactionLogger);
        }

        transactionLogger.setState(state);
        if (null != key && null != value) {
            transactionLogger.setKey(key);
            transactionLogger.setValue(value);
        }
        if (newtransaction) {
            this.dataObject.persistNewObject(transactionLogger);
        } else {
            this.dataObject.updateObject(transactionLogger);
        }

        this.dataObject.commit();
    }

    private Integer getTransactionID() throws InterruptedException {
        AtomicInteger transactionID = new AtomicInteger();
        try {
            synchronized (ongoingUserChanges) {
                if (ongoingUserChanges.get() > 0) {
                    ongoingUserChanges.wait();
                }
            }

            synchronized (currentTransactionID) {
                if (currentTransactionID.get() == 1000) {
                    currentTransactionID.set(1);
                } else {
                    currentTransactionID.incrementAndGet();
                }
                transactionID.set(currentTransactionID.get());
            }
        } catch (InterruptedException e) {
            logger.error("Transaction id couldn't be obtained.", e);
            throw e;
        }
        return transactionID.get();
    }


    Boolean pollAllReplicas(Integer transactionId, String key, String value) throws TimeoutException {
        this.changeTransactionLoggerState(transactionId, TransactionState.POLL, key, value);
        try {
            for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
                if (!entry.getValue().readyToCommit(transactionId, key, value)) {
                    return false;
                }
            }
        } catch (RemoteException e) {
            logger.error("stacktrace -- Polling ", e);
            return false;
        }
        return true;
    }

    void putAndConfirmCommit(Integer transactionID, String key, String value) throws NotFoundException, RemoteException {
        this.changeTransactionLoggerState(transactionID, TransactionState.ACTIONCHECK, key, value);
        try {

            for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
                entry.getValue().put(transactionID, key, value);
            }
            this.changeTransactionLoggerState(transactionID, TransactionState.COMMIT, key, value);
            for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
                entry.getValue().commit(transactionID);


            }
        } catch (RemoteException | NotFoundException e) {
            logger.error("stacktrace -- Polling ", e);
            throw e;
        }

    }


    void deleteAndConfirmCommit(Integer transactionID, String key, String value) throws NotFoundException, RemoteException {
        this.changeTransactionLoggerState(transactionID, TransactionState.ACTIONCHECK, key, value);
        try {

            for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
                entry.getValue().delete(transactionID, key);
            }
            this.changeTransactionLoggerState(transactionID, TransactionState.COMMIT, key, value);
            for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
                entry.getValue().commit(transactionID);

            }
        } catch (RemoteException | NotFoundException e) {
            logger.error("stacktrace -- Polling ", e);
            throw e;
        }

    }


    void sendAbortNotification(Integer transactionID) {
        for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
            try {
                entry.getValue().abortTransaction(transactionID);
            } catch (RemoteException e) {
                logger.error("Threw Remote exception. We wont do anything for it", e);
            }
        }
    }


    @Override

    public void put(String key, String value) throws RemoteException, TimeoutException, IllegalStateException {
        try {
            Integer transactionID = getTransactionID();
            changeTransactionLoggerState(transactionID, TransactionState.START, key, value);
            StampedLock lock;
            if (this.keyLockMap.containsKey(key)) {
                lock = this.keyLockMap.get(key);
            } else {
                lock = new StampedLock();
                this.keyLockMap.put(key, lock);

            }
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                try {
                    if (pollAllReplicas(transactionID, key, value)) {
                        putAndConfirmCommit(transactionID, key, value);
                    } else {
                        changeTransactionLoggerState(transactionID, TransactionState.ABORT, key, value);
                        sendAbortNotification(transactionID);
                        throw new IllegalStateException("Replica Couldn't accept a commit write now");
                    }
                } catch (Exception e) {
                    this.changeTransactionLoggerState(transactionID, TransactionState.ABORT, key, value);
                    sendAbortNotification(transactionID);
                    throw new IllegalStateException("Exception occurred at on of the clients.");
                } finally {
                    lock.unlockWrite(stamp);
                }
            } else {
                throw new IllegalStateException("Write Lock Not acquired");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            synchronized (ongoingTransactions) {
                if (ongoingTransactions.decrementAndGet() == 0) {
                    ongoingTransactions.notify();
                }
            }
        }
    }

    @Override
    public void delete(String key) throws RemoteException, TimeoutException {

    }

    @Override
    public String get(String key) throws RemoteException, NotFoundException {

        StampedLock lock;
        if (this.keyLockMap.containsKey(key)) {
            lock = this.keyLockMap.get(key);
        } else {
            lock = new StampedLock();
            this.keyLockMap.put(key, lock);

        }
        long stamp = lock.readLock();
        try {
            Object[] replicaArray = KvStoreMasterReplica.replicaInterfaceMap.keySet().toArray();
            Object randomKey = replicaArray[new Random().nextInt(replicaArray.length)];
            KvReplicaInterface kvReplicaInterface = KvStoreMasterReplica.replicaInterfaceMap.get(randomKey);
            return kvReplicaInterface.get(key);
        } finally {
            lock.unlockRead(stamp);
        }
    }
}


//TODO
//        1.make shutdown better because of exception
//        2.Make sure I make a table for ALl replicas connected at anygiven time
