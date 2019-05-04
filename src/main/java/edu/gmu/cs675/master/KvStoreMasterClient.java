package edu.gmu.cs675.master;


import edu.gmu.cs675.doa.DOA;
import edu.gmu.cs675.master.model.TransactionLogger;
import edu.gmu.cs675.shared.KvClientInterface;
import edu.gmu.cs675.shared.KvMasterReplicaInterface;
import edu.gmu.cs675.shared.KvReplicaInterface;
import edu.gmu.cs675.shared.TransactionState;
import javassist.NotFoundException;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
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


    KvStoreMasterClient() throws RemoteException {
        this.startRMIServer();
        this.keyLockMap = new ConcurrentHashMap<>();
        this.transactionLoggerMap = new ConcurrentHashMap<>();
        this.dataObject = DOA.getDoa();
        getFirstTransactionID();
        restore();
    }

    void restore() {
        try {
            List transactionLoggerSet = dataObject.getTransactionsNotComplete();
            if (transactionLoggerSet.size() > 0) {
                System.out.println("Restoring all the Transactions from previous slate");
                for (Object object : transactionLoggerSet) {
                    TransactionLogger logger = (TransactionLogger) object;
                    rollBackAbort(logger.getTransactionId());
                }
            }
        } catch (Exception ex) {
            logger.error("stackTrace ", ex);
            throw ex;
        }
    }

    public void startRMIServer() throws RemoteException {
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "100");
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

    /**
     * @param transactionID The transactionID of the transaction we log.
     * @param state         State of that transaction. Detailed transaction state list available at interface TransactionState
     * @param key           Can be null
     * @param value         Can be null
     * @param operation     Which operation is being performed.there are only 2 right now, put and delete.
     */
    private void changeTransactionLoggerState(Integer transactionID, Integer state, String key, String value, String operation) {
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
        if (null != operation) {
            operation = operation.toUpperCase();
            transactionLogger.setOperation(operation);
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

    private void getFirstTransactionID() {
        Integer transactionId = dataObject.getLastTransactionID();
        if (null != transactionId) {
            currentTransactionID.set(transactionId);
            currentTransactionID.incrementAndGet();
        }
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
        this.changeTransactionLoggerState(transactionId, TransactionState.POLL, key, value, null);
        System.out.println("\tPolling all replicas");
        try {
            for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
                if (!entry.getValue().readyToCommit(transactionId, key, value)) {
                    return false;
                }
            }
        } catch (RemoteException e) {
            logger.error("stacktrace -- Polling ", e);
            System.out.println("\tPolling un successful");
            return false;
        }
        System.out.println("\tPolling Successful !!!");
        return true;
    }

    void putAndConfirmCommit(Integer transactionID, String key, String value) throws NotFoundException, RemoteException {
        System.out.println("\tPutting the values in each replica");
        this.changeTransactionLoggerState(transactionID, TransactionState.ACTIONCHECK, key, value, null);
        try {
            for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
                entry.getValue().put(transactionID, key, value);
            }
            this.changeTransactionLoggerState(transactionID, TransactionState.COMMIT, key, value, null);
            for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
                entry.getValue().commit(transactionID);
            }
            changeTransactionLoggerState(transactionID, TransactionState.COMPLETE, key, value, null);
        } catch (RemoteException | NotFoundException e) {
            logger.error("stacktrace -- Polling ", e);
            System.out.println("\tPutting failed");
            throw e;
        }

    }


    void deleteAndConfirmCommit(Integer transactionID, String key) throws NotFoundException, RemoteException {
        this.changeTransactionLoggerState(transactionID, TransactionState.ACTIONCHECK, key, null, null);
        try {
            System.out.println("\tdeleting the values in each replica");
            for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
                entry.getValue().delete(transactionID, key);
            }
            this.changeTransactionLoggerState(transactionID, TransactionState.COMMIT, key, null, null);
            for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
                entry.getValue().commit(transactionID);
            }
            changeTransactionLoggerState(transactionID, TransactionState.COMPLETE, key, null, null);
        } catch (RemoteException | NotFoundException e) {
            logger.error("stacktrace -- Polling ", e);
            System.out.println("\tDeletion Failed");
            throw e;
        }

    }


    void sendAbortNotification(Integer transactionID) {

        for (Map.Entry<String, KvReplicaInterface> entry : KvStoreMasterReplica.replicaInterfaceMap.entrySet()) {
            try {
                System.out.println("\tSending Abort Signal");
                entry.getValue().abortTransaction(transactionID);
            } catch (RemoteException e) {
                logger.error("Threw Remote exception. We wont do anything for it", e);
            }
        }
    }

    void rollBackAbort(Integer transactionId) {
        String key = transactionLoggerMap.get(transactionId).getKey();
        TransactionLogger lastLogger = dataObject.getLastValidValue(key);
        if (lastLogger.getOperation().equalsIgnoreCase("put")) {
            put(key, lastLogger.getValue());
        } else {
            delete(key);
        }

        changeTransactionLoggerState(transactionId, TransactionState.ROLBACKCOMPLETE, key, null, lastLogger.getOperation());
    }

    @Override

    public void put(String key, String value) throws IllegalStateException {
        System.out.println("Put requested for key :" + key);
        try {
            Integer transactionID = getTransactionID();
            changeTransactionLoggerState(transactionID, TransactionState.START, key, value, "put");
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
                        changeTransactionLoggerState(transactionID, TransactionState.ABORT, key, value, "put");
                        sendAbortNotification(transactionID);
                        rollBackAbort(transactionID);
                        throw new IllegalStateException("Replica Couldn't accept a commit write now");
                    }
                } catch (Exception e) {
                    this.changeTransactionLoggerState(transactionID, TransactionState.ABORT, key, value, "put");
                    sendAbortNotification(transactionID);
                    rollBackAbort(transactionID);
                    throw new IllegalStateException("Exception occurred at on of the clients.");
                } finally {
                    lock.unlockWrite(stamp);
                }
            } else {
                throw new IllegalStateException("Write Lock Not acquired");
            }
        } catch (InterruptedException e) {
            logger.error("StackTrace -", e);
            throw new IllegalStateException("Couldn't put right now");
        } finally {
            synchronized (ongoingTransactions) {
                if (ongoingTransactions.decrementAndGet() == 0) {
                    ongoingTransactions.notify();
                }
            }
        }
    }

    @Override
    public void delete(String key) throws IllegalStateException {
        System.out.println("Delete requested for key :" + key);
        try {
            Integer transactionID = getTransactionID();
            changeTransactionLoggerState(transactionID, TransactionState.START, key, null, "delete");
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
                    if (pollAllReplicas(transactionID, key, null)) {
                        deleteAndConfirmCommit(transactionID, key);
                    } else {
                        changeTransactionLoggerState(transactionID, TransactionState.ABORT, key, null, "delete");
                        sendAbortNotification(transactionID);
                        rollBackAbort(transactionID);
                        throw new IllegalStateException("Replica Couldn't accept a commit write now");
                    }
                } catch (Exception e) {
                    this.changeTransactionLoggerState(transactionID, TransactionState.ABORT, key, null, "delete");
                    sendAbortNotification(transactionID);
                    rollBackAbort(transactionID);
                    throw new IllegalStateException("Exception occurred at on of the clients.");
                } finally {
                    lock.unlockWrite(stamp);
                }
            } else {
                throw new IllegalStateException("Write Lock Not acquired");
            }
        } catch (InterruptedException e) {
            logger.error("StackTrace -", e);
            throw new IllegalStateException("Couldn't delete right now");
        } finally {
            synchronized (ongoingTransactions) {
                if (ongoingTransactions.decrementAndGet() == 0) {
                    ongoingTransactions.notify();
                }
            }
        }
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
