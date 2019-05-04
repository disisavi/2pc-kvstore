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

        Registry gameRegistry = getRegistry(host, KvClientInterface.port);
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
        }
    }

    void delete(String key) {
        try {
            this.kvClientInterface.delete(key);
            System.out.println("Key " + key + " is deleted");
        } catch (RemoteException | TimeoutException e) {
            System.out.println("Couldn't get because " + e.getMessage());
        } catch (Exception ex) {
            System.out.println("oops, the operation failed");
        }
    }

    void run() {
        boolean run = true;
        this.showAvailableCommands();
        Scanner scanner = new Scanner(System.in);
        while (run) {
            String[] command = scanner.nextLine().split(" ", 2);
            try {
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
                    case "DELETE":
                        if (command.length == 1) {
                            System.out.println("please enter the command in correct format");
                            break;
                        } else {
                            String[] insertParam = command[1].split(" ", 0);
                            this.delete(insertParam[0]);
                            break;
                        }
                    case "show":
                        this.showAvailableCommands();
                        break;
                    default:
                        System.out.println("Please enter one of the printed commands");
                        this.showAvailableCommands();
                }
            } catch (Exception ex) {
                System.out.println("Operation failed.. " + ex.getMessage());
            }
        }
    }

    void showAvailableCommands() {
        System.out.println("\n#####################");
        System.out.println("The following commands have been implemented at Client");
        System.out.println("1.Put -     |-> put \"key\" \"value\"");
        System.out.println("2.Get       |--> get \"key\"");
        System.out.println("1.delete    |--> delete \"key\"");
        System.out.println("Bonus Commands");
        System.out.println("4. Clear    |--> To Clear the console, so that we may better see the results.");
        System.out.println("5. Show     |--> To show list of all available commands");
        System.out.println("Exit --> To exit\nPlease type in the command...");

    }

    void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void main(String[] args) {
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
