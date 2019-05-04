package edu.gmu.cs675.doa;

import edu.gmu.cs675.master.model.TransactionLogger;
import edu.gmu.cs675.shared.TransactionState;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DOA {
    private static DOA doa;
    final static Logger logger = Logger.getLogger(DOA.class);
    private Session session;
    private Transaction transaction;

    private DOA() {
        this.session = HibernateUtil.getSessionFactory().openSession();
        transaction = this.session.beginTransaction();

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

    void getNewSession() {
        this.session = HibernateUtil.getSessionFactory().openSession();
        transaction = this.session.beginTransaction();
    }

    void startTrasaction() {
        transaction.begin();
    }

    public Object getByKey(Class className, String key) {
        if (!this.transaction.isActive()) {
            this.startTrasaction();
        }
        Object object = this.session.get(className, key);
        this.commit();
        return object;
    }

    public Integer getLastTransactionID() {
        Criteria criteria = session.createCriteria(TransactionLogger.class);
        criteria.setProjection(Projections.max("transactionId"));

        Integer returnResult = (Integer) criteria.uniqueResult();

        commit();
        return returnResult;
    }

    public List getTransactionsNotComplete() {
        Criteria criteria = session.createCriteria(TransactionLogger.class);
        criteria.add(Restrictions.eq("state", TransactionState.ACTIONCHECK));
        criteria.add(Restrictions.eq("state", TransactionState.COMMIT));
        criteria.add(Restrictions.eq("state", TransactionState.ABORT));
        List transactionLoggerList = criteria.list();
        commit();
        return transactionLoggerList;
    }

    public TransactionLogger getLastValidValue(String key) {
        Criteria criteria = session.createCriteria(TransactionLogger.class);
        criteria.add(Restrictions.eq("key", key));
        criteria.add(Restrictions.eq("state", TransactionState.COMPLETE));
        criteria.addOrder(Order.desc("transactionId"));
        criteria.setMaxResults(1);
        TransactionLogger logger = (TransactionLogger) criteria.uniqueResult();
        commit();
        return logger;
    }

    public void updateObject(Object object) {
        if (!this.transaction.isActive()) {
            this.startTrasaction();
        }
        this.session.update(object);
    }

    public synchronized void persistNewObject(Object object) {
        if (!this.transaction.isActive()) {
            this.startTrasaction();
        }
        this.session.persist(object);
    }

    public synchronized void removeObject(Object object) {
        if (!this.transaction.isActive()) {
            this.startTrasaction();
        }
        this.session.delete(object);
    }

    public Set<Object> getAll(Class classname) {
        if (!this.transaction.isActive()) {
            this.startTrasaction();
        }

        List objects = this.session.createCriteria(classname).list();
        Set<Object> retrunObjectSet = new HashSet<>(objects);

        this.commit();
        return retrunObjectSet;


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
        try {
            this.session.getTransaction().commit();
            this.getNewSession();
        } catch (Exception e) {
            this.session.getTransaction().rollback();
            throw e;
        }
    }
}
