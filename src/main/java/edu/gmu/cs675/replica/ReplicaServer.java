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
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ReplicaServer {
    static Logger logger = Logger.getLogger(ReplicaServer.class);
    InetAddress selfIp;
    String hostname;
    KvReplicaInterface kvClientInterface;
    Replica replica;


    private ReplicaServer() {
        try {
            selfIp = getSelfIP();
        } catch (SocketException | UnknownHostException e) {
            logger.error("Couldn't Obtain Self IP as" + e.getMessage());
            logger.error("stackTrace-- ", e);
            logger.info("Aborting Mission");
        }
    }

    private InetAddress getSelfIP() throws SocketException, UnknownHostException {

        final DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 80);
        InetAddress ip = InetAddress.getByName(socket.getLocalAddress().getHostAddress());

        return ip;
    }

    public void startRMIServer() {

        try {
            Replica replica = new Replica();
            this.replica = replica;
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(KvReplicaInterface.port);
            } catch (RemoteException e) {
                logger.info("Unable to create registry.... Checking if registry already exist");
                registry = LocateRegistry.getRegistry(KvReplicaInterface.port);
            }
            KvReplicaInterface nodeStub = (KvReplicaInterface) UnicastRemoteObject.exportObject(replica, KvReplicaInterface.port);
            registry.rebind(KvReplicaInterface.hostname, nodeStub);

            this.hostname = KvReplicaInterface.hostname;
            replica.selfStub = nodeStub;
            replica.replicaStartup();//change to startup;

            System.out.println("ip -- " + selfIp.getHostAddress());
            logger.info("KV Store Complete\nmaster Name -- " + hostname);
            logger.info("ip -- " + selfIp.getHostAddress());
        } catch (Exception e) {
            System.out.println("KV Store Startup Failure ... Proceeding to shutdown");
            logger.error("KV Store Startup Failure ...");
            shutdown(e);
        }
    }


    private void shutdown(Exception exception) {
        // Think about making 1 shutdown method for all servers
        System.out.println("Shutting down the replica");
        logger.info("Shutting down the replica");
        if (exception != null) {
            logger.error("The following error lead to the shutdown " + exception.getMessage());
            logger.error("Full StackTrace is as Follows", exception);
        }
        try {
            if (this.replica != null) {
                this.replica.shutdown();
            }
        } catch (Exception ex) {
            logger.error("Stack tracke", ex);
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
            try {
                switch (command[0].toUpperCase()) {
                    case "EXIT":
                        this.shutdown(new Exception("Server Requested Shutdown"));
                        break;

                    case "SHOW":
                        HashMap<String, String> keyValue = this.replica.getAll();
                        System.out.println("All the values are");
                        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
                            System.out.println("\tKey, Value ->" + entry.getKey() + ", " + entry.getValue());
                        }
                        break;

                    default:
                        System.out.println("Please enter one of the printed commands");
                        this.showAvailableComands();
                }
            } catch (Exception e) {
                System.out.println("Action " + command[0].toUpperCase() + " could not be performed");
                logger.error("Error -- ", e);
            }
        }
    }

    private void showAvailableComands() {
        System.out.println("\n#####################");
        System.out.println("\nReplica server is up and running");
        System.out.println("\n********************");
        System.out.println("\nFollowing commands are available");
        System.out.println("1. Show --> Get all the values in this replica right now");
        System.out.println("2. Exit");
    }

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        try {
            ReplicaServer ser = new ReplicaServer();
            ser.startRMIServer();
            ser.run();
        } catch (Exception e) {
            logger.error("Exception thrown at startup of replica." + e.getMessage());
            logger.error("stackTrace -- ", e);
            System.out.println("Error " + e.getMessage());
            System.out.println("Shutting down");
            logger.info("Exiting the replica");
        }
    }
}

