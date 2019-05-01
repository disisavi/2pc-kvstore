package edu.gmu.cs675.client;

import edu.gmu.cs675.shared.KvClientInterface;
import javassist.NotFoundException;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import static java.rmi.registry.LocateRegistry.getRegistry;

public class Client {
    KvClientInterface kvClientInterface;

    Client() throws RemoteException, NotBoundException {
        kvClientInterface = getStub();
    }

    public KvClientInterface getStub() throws RemoteException, NotBoundException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the Game server ip ");
        String host = scanner.next();

        Registry gameRegistry = getRegistry(KvClientInterface.name, KvClientInterface.port);
        KvClientInterface kvClientInterface = (KvClientInterface) gameRegistry.lookup(KvClientInterface.name);
        return kvClientInterface;
    }

    void put(String key, String value) {
        try {
            this.kvClientInterface.put(key, value);
        } catch (RemoteException | TimeoutException e) {
            System.out.println("Couldnt be completed because " + e.getMessage());

        }
    }

    void get(String key) {
        try {
            String string = this.kvClientInterface.get(key);
            System.out.println("Value is " + string);
        } catch (RemoteException | NotFoundException e) {
            System.out.println("Couldn't get because " + e.getMessage());
            e.printStackTrace();
        }
    }

    void run() {
        boolean run = true;
        this.showAvailableCommands();
        Scanner scanner = new Scanner(System.in);
        while (run) {
            String[] command = scanner.nextLine().split(" ", 2);

            switch (command[0].toUpperCase()) {
                case "CLEAR":
                    this.clearConsole();
                    break;
                case "PUT":
                    if (command.length == 1) {
                        System.out.println("please enter the command in correct format");
                        break;
                    } else {
                        String[] insertParam = command[1].split(" ", 0);
                        if (insertParam.length == 1) {
                            System.out.println("Kindly follow the correct Format of Insert command");
                            break;
                        }

                        this.put(insertParam[0], insertParam[1]);
                        break;
                    }
                case "GET":
                    if (command.length == 1) {
                        System.out.println("please enter the command in correct format");
                        break;
                    } else {
                        String[] insertParam = command[1].split(" ", 0);
                        this.get(insertParam[0]);
                        break;
                    }
                default:
                    System.out.println("Please enter one of the printed commands");
                    this.showAvailableCommands();
            }
        }
    }

    void showAvailableCommands() {
        System.out.println("\n1.Put");
        System.out.println("2. Get");
    }

    void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void main() {
        try {
            Client client = new Client();
            client.clearConsole();
            client.run();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

}
