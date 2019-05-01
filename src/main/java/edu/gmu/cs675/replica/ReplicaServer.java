package edu.gmu.cs675.replica;

import edu.gmu.cs675.shared.KvMasterReplicaInterface;
import edu.gmu.cs675.shared.KvReplicaInterface;
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
import java.util.Scanner;

public class ReplicaServer {
    static Logger logger = Logger.getLogger(edu.gmu.cs675.master.server.class);
    InetAddress selfIp;
    String hostname;
    KvReplicaInterface kvClientInterface;
    Replica kvStoreMaster;


    private ReplicaServer() {
        try {
            selfIp = getSelfIP();

        } catch (SocketException | UnknownHostException e) {
            logger.error("Couldn't Obtain Self IP as" + e.getMessage());
            logger.error(e);
            logger.info("Aborting Mission");
        }
        hostname = selfIp.getHostName();
    }

    private InetAddress getSelfIP() throws SocketException, UnknownHostException {

        final DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), KvMasterReplicaInterface.port);
        InetAddress ip = InetAddress.getByName(socket.getLocalAddress().getHostAddress());

        return ip;
    }

    public void startRMIServer() {

        try {
            Replica kvStoreMasterClient = new Replica();

            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(KvMasterReplicaInterface.port);
            } catch (RemoteException e) {
                logger.info("Unable to create registry.... Checking if registry already exist");
                registry = LocateRegistry.getRegistry(KvMasterReplicaInterface.port);
            }
            KvReplicaInterface nodeStub = (KvReplicaInterface) UnicastRemoteObject.exportObject(kvStoreMasterClient, KvReplicaInterface.port);

            registry.rebind(this.hostname, nodeStub);
            System.out.println("KV Store Complete\nmaster Name -- " + hostname);
            System.out.println("ip -- " + selfIp.getHostAddress());
            logger.info("KV Store Complete\nmaster Name -- " + hostname);
            logger.info("ip -- " + selfIp.getHostAddress());
        } catch (RemoteException | NotBoundException e) {
            System.out.println("KV Store Startup Failure ... Proceeding to shutdown");
            logger.error("KV Store Startup Failure ...");
            shutdown(e);
        }
    }


    private void shutdown(Exception exception) {
        System.out.println("Shutting down Persistent KV store master");
        logger.info("Shutting down Persistent KV store master");
        if (exception != null) {
            logger.error("The following error lead to the shutdown");
            logger.error(exception.getMessage());
            logger.error("Full StackTrace is as Follows", exception);
        }

        if (this.kvStoreMaster != null) {
            this.kvStoreMaster.shutdown();
        }
        try {

            Registry registry = LocateRegistry.getRegistry();
            registry.unbind(this.hostname);
            UnicastRemoteObject.unexportObject(this.kvClientInterface, true);
            Runtime.getRuntime().gc();
        } catch (RemoteException | NotBoundException e) {
            logger.error(e);
            logger.error("Stack trace ", e);
        }

        // otherwise we wait 60seconds for references to be removed
        Runtime.getRuntime().gc();
        System.exit(-1);
    }

    void run() {
        Scanner scanner = new Scanner(System.in);
        boolean runAlways = true;
        this.showAvailableComands();
        while (runAlways) {
            String argumet = scanner.nextLine();
            String[] command = argumet.trim().split(" ", 2);
            switch (command[0].toUpperCase()) {
                case "EXIT":
                    this.shutdown(new Exception("Server Requested Shutdown"));
                default:
                    System.out.println("Please enter one of the printed commands");
                    this.showAvailableComands();
            }
        }
    }

    void showAvailableComands() {
        System.out.println("\n#####################");
        System.out.println("\nFollowing commands are available");
        System.out.println("1. Exit");
    }

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        try {
            ReplicaServer ser = new ReplicaServer();
            ser.startRMIServer();
            ser.run();
        } catch (Exception e) {
            logger.error(e);
            logger.error(e.getStackTrace());
        }
    }
}

