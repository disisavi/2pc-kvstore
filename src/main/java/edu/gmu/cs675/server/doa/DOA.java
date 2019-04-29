package edu.gmu.cs675.server.doa;

import org.apache.log4j.Logger;
import org.hibernate.Session;

public class DOA {
    private static DOA doa;
    final static Logger logger = Logger.getLogger(DOA.class);
    private Session session;

    private DOA() {
        this.session = HibernateUtil.getSessionFactory().openSession();
        this.session.beginTransaction();
    }

    public static DOA getDoa() {
        if (doa == null) {
            synchronized (DOA.class) {
                if (doa == null) {
                    doa = new DOA();
                }
            }
        }
        return doa;
    }

    void startTrasaction() {
        this.session.getTransaction().begin();
    }

    public Object getByKey(Class className, String key) {
        if (!this.session.getTransaction().isActive()) {
            this.startTrasaction();
        }
        Object object = this.session.get(className, key);
        return object;
    }

    public synchronized void persistNewObject(Object object) {
        if (!this.session.getTransaction().isActive()) {
            this.startTrasaction();
        }
        this.session.persist(object);
    }

    public synchronized void removeObject(Object object) {
        this.session.delete(object);
    }

    public void shutdown() {
        this.session.close();
        try {
            HibernateUtil.close();
        } catch (Exception e) {
            logger.error("Couldn't close the connection as " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void commit() {
        this.session.getTransaction().commit();
    }
}
