package edu.gmu.cs675.server;

import edu.gmu.cs675.server.doa.HibernateUtil;
import org.hibernate.Session;
import edu.gmu.cs675.server.model.keyValuePersistence;

public class server {
    public static void main(String[] args){
        System.out.println("Lets start");
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        keyValuePersistence keyValuePersistence = new keyValuePersistence("sravya","too much wow");
        session.save(keyValuePersistence);
        session.getTransaction().commit();
        session.close();

    }
}
