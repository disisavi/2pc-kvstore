package edu.gmu.cs675.server;

import edu.gmu.cs675.server.doa.DOA;
import edu.gmu.cs675.server.model.KeyValuePersistence;
import edu.gmu.cs675.shared.KvMasterInterface;
import edu.gmu.cs675.shared.KvReplicaInterface;
import javassist.NotFoundException;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;


class KvStoreMaster implements KvMasterInterface {

    private static Logger logger = Logger.getLogger(KvStoreMaster.class);
    Map<String, KvReplicaInterface> clientMap;
    Map<String, KvClass> kvMap;
    DOA dataObject;
    final AtomicInteger ongoingTransactions;
    ReentrantReadWriteLock transactionClientLock;

    KvStoreMaster() {
        logger.info("Master initialisation started...");
        this.clientMap = new ConcurrentHashMap<>();
        this.kvMap = new ConcurrentHashMap<>();
        this.ongoingTransactions = new AtomicInteger(0);
        this.dataObject = DOA.getDoa();
        this.transactionClientLock = new ReentrantReadWriteLock();
        logger.info("Master initialisation Successful");
    }


    void shutdown() {
        this.dataObject.shutdown();
    }

    @Override
    public void registerClient(KvReplicaInterface kvClient) throws IllegalArgumentException, RemoteException {
     /*   TODO
                1. Check If we need to synchronize indivisual elements elements in concurrentMap
      */
        try {
            if (this.ongoingTransactions.get() > 0)
                synchronized (this.ongoingTransactions) {
                    this.ongoingTransactions.wait();
                }
            this.transactionClientLock.readLock().lock();
            try {
                String hostname = RemoteServer.getClientHost();
                if (this.clientMap.containsKey(hostname)) {
                    logger.info("The client " + hostname + " is already registered");
                    logger.info("Throwing exception to the Client");
                    throw new IllegalArgumentException("Host Already Registered.. Please Deregister and try again.");
                }
                this.clientMap.put(hostname, kvClient);

            } catch (ServerNotActiveException e) {
                logger.error("Cannot get client Hostname.. " + e.getMessage() + " at thread " + Thread.currentThread().getId());
                logger.error("StackTrace", e);
            } finally {
                this.transactionClientLock.readLock().unlock();
            }
        } catch (InterruptedException e) {
            logger.error("Thread interrupted while waiting. Im still not sure why that might be and what actions to take");
            logger.error(e.getMessage());
            logger.error("StackTrace", e);
            throw new RemoteException("Action Not performed");
        }
    }

    @Override
    public void deRegisterClient() throws RemoteException, IllegalArgumentException {
        try {
            if (this.ongoingTransactions.get() > 0)
                synchronized (this.ongoingTransactions) {
                    this.ongoingTransactions.wait();
                }
            this.transactionClientLock.readLock().lock();
            try {
                String hostname = RemoteServer.getClientHost();
                if (this.clientMap.containsKey(hostname)) {
                    this.clientMap.remove(hostname);
                } else {
                    throw new IllegalArgumentException("Host Doesnt seem to be initialised. Kindly register the client");
                }
            } catch (ServerNotActiveException e) {
                logger.error("Cannot get client Hostname.. " + e.getMessage() + " at thread " + Thread.currentThread().getId());
                logger.error("StackTrace", e);
            } finally {
                this.transactionClientLock.readLock().unlock();
            }
        } catch (InterruptedException e) {
            logger.error("Thread interrupted while waiting. Im still not sure why that might be and what actions to take");
            logger.error(e.getMessage());
            logger.error("StackTrace", e);
            throw new RemoteException("Action Not performed");
        }
    }

    @Override
    public Integer getTransactionID() throws RemoteException {
        return null;
    }

    @Override
    public Long getLockForKey(Boolean readWriteFlag, String key, Integer transactionID) throws RemoteException, NotFoundException {
        return null;
    }

    @Override
    public void giveLoclForKey(Boolean readWriteFlag, String key, Integer transactionID) throws RemoteException, NotFoundException {

    }

    @Override
    public void put(Integer transactionID, String key, String value) throws RemoteException {

    }

    @Override
    public void delete(Integer transactionID, String key) throws RemoteException {

    }

    @Override
    public String get(String key) throws RemoteException, NotFoundException {
        return null;
    }

    @Override
    public Map<String, String> getKVStream() throws RemoteException {
        return null;
    }

    class KvClass {
        String key;
        KeyValuePersistence keyValuePersistence;
        Boolean readLocked;
        Boolean writeLock;
        Boolean dirty;
        Long lockValue;


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
