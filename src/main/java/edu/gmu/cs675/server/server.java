package edu.gmu.cs675.server;


import org.apache.log4j.Logger;

public class server {
    static Logger logger = Logger.getLogger(server.class);

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        try {
            KvStoreMaster.startRMIServer();
        } catch (Exception e) {
            logger.error(e);
            logger.error(e.getStackTrace());
        }
    }
}
