package edu.gmu.cs675.master.doa;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public void updateObject(Object object) {
        if (!this.session.getTransaction().isActive()) {
            this.startTrasaction();
        }
        this.session.update(object);
    }

    public synchronized void persistNewObject(Object object) {
        if (!this.session.getTransaction().isActive()) {
            this.startTrasaction();
        }
        this.session.persist(object);
    }

    public synchronized void removeObject(Object object) {
        if (!this.session.getTransaction().isActive()) {
            this.startTrasaction();
        }
        this.session.delete(object);
    }

    public Set<Object> getAll(Class classname) {
        if (!this.session.getTransaction().isActive()) {
            this.startTrasaction();
        }

        List objects = this.session.createCriteria(classname).list();
        return new HashSet<>(objects);


    }

    public void shutdown() {
        this.session.close();
        try {
            HibernateUtil.close();
        } catch (Exception e) {
            logger.error("Couldn't close the connection as " + e.getMessage());
            logger.error("StackTrace -- ", e);
        }
    }

    public void commit() {
        this.session.getTransaction().commit();
    }
}
