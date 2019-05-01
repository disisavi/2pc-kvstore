package edu.gmu.cs675.master;


import edu.gmu.cs675.doa.DOA;
import edu.gmu.cs675.shared.KvMasterReplicaInterface;
import edu.gmu.cs675.shared.KvReplicaInterface;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


class KvStoreMasterReplica implements KvMasterReplicaInterface {

    private static Logger logger = Logger.getLogger(KvStoreMasterReplica.class);
    static Map<String, KvReplicaInterface> replicaInterfaceMap;
    private DOA dataObject;

    KvStoreMasterReplica() {
        logger.info("Master initialisation started...");
        this.dataObject = DOA.getDoa();
        replicaInterfaceMap = new ConcurrentHashMap<>();
        logger.info("Master initialisation Successful");
    }


    void shutdown() {
        this.dataObject.shutdown();
    }

    @Override
    public Map<String, String> registerReplica(KvReplicaInterface kvClient) throws IllegalArgumentException, RemoteException {
     /*   TODO
                1. Check If we need to synchronize indivisual elements elements in concurrentMap
      */
        try {
            synchronized (KvStoreMasterClient.ongoingTransactions) {
                if (KvStoreMasterClient.ongoingTransactions.get() > 0) {
                    KvStoreMasterClient.ongoingTransactions.wait();
                }
            }
            KvStoreMasterClient.ongoingTransactions.incrementAndGet();
            try {
                String hostname = RemoteServer.getClientHost();
                if (replicaInterfaceMap.containsKey(hostname)) {
                    logger.info("The client " + hostname + " is already registered");
                    logger.info("Throwing exception to the Client");
                    throw new IllegalArgumentException("Host Already Registered.. Please Deregister and try again.");
                }
                Map<String, String> value = this.fetchAll();
                replicaInterfaceMap.put(hostname, kvClient);
                return value;
            } catch (ServerNotActiveException e) {
                logger.error("Cannot get client Hostname.. " + e.getMessage() + " at thread " + Thread.currentThread().getId());
                logger.error("StackTrace", e);
                throw new RemoteException("Action Not performed");
            }
        } catch (InterruptedException e) {
            logger.error("Thread interrupted while waiting. Im still not sure why that might be and what actions to take");
            logger.error(e.getMessage());
            logger.error("StackTrace", e);
            throw new RemoteException("Action Not performed");
        } finally {
            synchronized (KvStoreMasterClient.ongoingUserChanges) {
                if (0 == KvStoreMasterClient.ongoingUserChanges.decrementAndGet()) {
                    KvStoreMasterClient.ongoingUserChanges.notify();
                }
            }
        }
    }

    @Override
    public void deRegisterReplica() throws RemoteException, IllegalArgumentException {
        try {
            synchronized (KvStoreMasterClient.ongoingTransactions) {
                if (KvStoreMasterClient.ongoingTransactions.get() > 0) {
                    KvStoreMasterClient.ongoingTransactions.wait();
                }
            }
            KvStoreMasterClient.ongoingUserChanges.incrementAndGet();
            try {
                String hostname = RemoteServer.getClientHost();
                if (replicaInterfaceMap.containsKey(hostname)) {
                    replicaInterfaceMap.remove(hostname);
                } else {
                    throw new IllegalArgumentException("Host Doesnt seem to be initialised. Kindly register the client");
                }
            } catch (ServerNotActiveException e) {
                logger.error("Cannot get client Hostname.. " + e.getMessage() + " at thread " + Thread.currentThread().getId());
                logger.error("StackTrace", e);
            }
        } catch (InterruptedException e) {
            logger.error("Thread interrupted while waiting. Im still not sure why that might be and what actions to take");
            logger.error(e.getMessage());
            logger.error("StackTrace", e);
            throw new RemoteException("Action Not performed");
        } finally {
            synchronized (KvStoreMasterClient.ongoingUserChanges) {
                if (0 == KvStoreMasterClient.ongoingUserChanges.decrementAndGet()) {
                    KvStoreMasterClient.ongoingUserChanges.notify();
                }
            }
        }
    }

    Map<String, String> fetchAll() throws RemoteException {

        if (replicaInterfaceMap.size() == 0) {
            return new HashMap<>();
        }

        Object[] replicaArray = replicaInterfaceMap.keySet().toArray();
        Object randomKey = replicaArray[new Random().nextInt(replicaArray.length)];
        KvReplicaInterface kvReplicaInterface = replicaInterfaceMap.get(randomKey);
        try {
            return kvReplicaInterface.getAll();
        } catch (RemoteException e) {
            logger.error("Remote exception to be thrown, Deatiled error are ", e);
            throw e;

        }
    }
}
