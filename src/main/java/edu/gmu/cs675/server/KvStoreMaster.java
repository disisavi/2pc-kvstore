package edu.gmu.cs675.server;

import edu.gmu.cs675.server.doa.DOA;
import edu.gmu.cs675.server.model.KeyValuePersistence;
import edu.gmu.cs675.shared.KvInterface;
import org.apache.log4j.Logger;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class KvStoreMaster implements KvInterface {

    private InetAddress selfIp;
    private String hostname;
    private static Logger logger = Logger.getLogger(KvStoreMaster.class);
    private Map<String, Long> keyLockMap;
    private DOA dataObjet;

    private KvStoreMaster() {
        try {
            selfIp = getSelfIP();
        } catch (SocketException | UnknownHostException e) {
            logger.error("Couldnt Obtain Self IP as" + e.getMessage());
            logger.error(e);
            logger.info("Aborting Mission");
        }
        hostname = selfIp.getHostName();
        keyLockMap = new ConcurrentHashMap<>();

        this.dataObjet = DOA.getDoa();
    }

    private InetAddress getSelfIP() throws SocketException, UnknownHostException {

        final DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), KvInterface.port);
        InetAddress ip = InetAddress.getByName(socket.getLocalAddress().getHostAddress());

        return ip;
    }

    public static void startRMIServer() {
        KvStoreMaster kvStoreMaster = new KvStoreMaster();
        try {

            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(KvInterface.port);
            } catch (RemoteException e) {
                logger.info("Unable to create registry.... Checking if registry already exist");
                registry = LocateRegistry.getRegistry(KvInterface.port);
            }
            KvInterface nodeStub = (KvInterface) UnicastRemoteObject.exportObject(kvStoreMaster, KvInterface.port);

            registry.rebind("game", nodeStub);
            logger.info("KV Store Complete\nserver Name -- " + kvStoreMaster.hostname);
            logger.info("ip -- " + kvStoreMaster.selfIp.getHostAddress());
        } catch (RemoteException e) {
            logger.info("KV Store Startup Failure ...");
            kvStoreMaster.shutdown(e);
        }
    }

    private void shutdown(Exception exception) {
        System.out.println("Shutting down Game RMI server");
        if (exception != null) {
            logger.error("The following error lead to the shutdown");
            logger.error(exception.getMessage());
            logger.error(exception);
            exception.printStackTrace();
        }
        try {

            Registry registry = LocateRegistry.getRegistry();
            registry.unbind(this.hostname);
            UnicastRemoteObject.unexportObject(this, true);
            Runtime.getRuntime().gc();
        } catch (RemoteException | NotBoundException e) {
            logger.error(e);
            e.printStackTrace();
        }

        dataObjet.shutdown();

        // otherwise we wait 60seconds for references to be removed
        Runtime.getRuntime().gc();
        System.exit(-1);
    }


    @Override
    public void put(String key, String value) throws RemoteException {
        KeyValuePersistence kv = new KeyValuePersistence(key, value);
        this.dataObjet.persistNewObject(kv);
        this.dataObjet.commit();
    }

    @Override
    public void delete(String key) {
        Object object = this.dataObjet.getByKey(KeyValuePersistence.class, key);
        this.dataObjet.removeObject(object);
        this.dataObjet.commit();

    }

    @Override
    public String get(String key) throws RemoteException {
        return (String) this.dataObjet.getByKey(KeyValuePersistence.class, key);
    }
}
