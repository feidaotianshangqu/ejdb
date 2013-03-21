package org.ejdb.driver;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public class EJDBCollection {

    /**
     * Drop index.
     */
    public final static int JBIDXDROP = 1 << 0;
    /**
     * Drop index for all types.
     */
    public final static int JBIDXDROPALL = 1 << 1;
    /**
     * Optimize index.
     */
    public final static int JBIDXOP = 1 << 2;
    /**
     * Rebuild index.
     */
    public final static int JBIDXREBLD = 1 << 3;
    /**
     * Number index.
     */
    public final static int JBIDXNUM = 1 << 4;
    /**
     * String index.
     */
    public final static int JBIDXSTR = 1 << 5;
    /**
     * Array token index.
     */
    public final static int JBIDXARR = 1 << 6;
    /**
     * Case insensitive string index
     */
    public final static int JBIDXISTR = 1 << 7;

    // transaction control options (inner use only)
    protected final static int JBTXBEGIN = 1 << 0;
    protected final static int JBTXCOMMIT = 1 << 1;
    protected final static int JBTXROLLBACK = 1 << 2;
    protected final static int JBTXSTATUS = 1 << 3;

    private EJDB db;
    private String cname;
    private boolean exists;
    private Options options;
    private Collection<Index> indexes;

    EJDBCollection(EJDB db, String cname) {
        this.db = db;
        this.cname = cname;
    }

    /**
     * @return EJDB object
     */
    public EJDB getDB() {
        return db;
    }

    /**
     * @return collection name
     */
    public String getName() {
        return cname;
    }

    /**
     * @return collection exists status
     */
    public boolean isExists() {
        return exists;
    }

    /**
     * @return collection options {@link Options}
     */
    public Options getOptions() {
        return options;
    }

    /**
     * @return indexes info
     */
    public Collection<Index> getIndexes() {
        return indexes;
    }

    /**
     * Automatically creates new collection if it does't exists with using default collection options.
     * @see EJDBCollection#ensureExists(org.ejdb.driver.EJDBCollection.Options)
     *
     * @throws EJDBException
     */
    public void ensureExists() throws EJDBException {
        this.ensureExists(null);
    }

    /**
     * Automatically creates new collection if it does't exists.
     * Collection options `opts` are applied only for newly created collection.
     * For existing collections `opts` takes no effect.
     *
     * @param opts Collection options.
     * @throws EJDBException
     */
    public native void ensureExists(Options opts) throws EJDBException;

    /**
     * Drop collection.
     *
     * @throws EJDBException
     */
    public void drop() throws EJDBException {
        this.drop(false);
    }

    /**
     * Drop collection.
     *
     * @param prune If true the collection data will erased from disk.
     * @throws EJDBException
     */
    public native void drop(boolean prune) throws EJDBException;

    /**
     * Synchronize entire collection with storage.
     *
     * @throws EJDBException
     */
    public native void sync() throws EJDBException;

    /**
     * Update collection metainformation from storage
     *
     * @throws EJDBException
     */
    public native void updateMeta() throws EJDBException;

    /**
     * Loads BSON object identified by OID from the collection.
     *
     * @param oid Object identifier (OID)
     * @throws EJDBException
     */
    public native BSONObject load(ObjectId oid) throws EJDBException;

    /**
     * Save/update specified BSON object in the collection.
     * <p/>
     * Object has unique identifier (OID) placed in the `_id` property.
     * If a saved object does not have `_id` it will be autogenerated.
     * To identify and update object it should contains `_id` property.
     * <p/>
     * NOTE: Field names of passed BSON objects may not contain `$` and `.` characters,
     * error condition will be fired in this case.
     *
     * @param object BSON object to save
     * @return OID of saved object
     * @throws EJDBException
     */
    public native ObjectId save(BSONObject object) throws EJDBException;

    /**
     * Save/update specified BSON objects in the collection.
     * <p/>
     * Each persistent object has unique identifier (OID) placed in the `_id` property.
     * If a saved object does not have `_id` it will be autogenerated.
     * To identify and update object it should contains `_id` property.
     * <p/>
     * NOTE: Field names of passed BSON objects may not contain `$` and `.` characters,
     * error condition will be fired in this case.
     *
     * @param objects array of JSON objects to save
     * @return OIDs of saved objects
     * @throws EJDBException
     */
    public List<ObjectId> save(List<BSONObject> objects) throws EJDBException {
        List<ObjectId> result = new ArrayList<ObjectId>(objects.size());

        for (BSONObject object : objects) {
            result.add(this.save(object));
        }

        return result;
    }

    /**
     * Remove BSON object from collection by OID
     *
     * @param oid OID of removed object
     * @throws EJDBException
     */
    public native void remove(ObjectId oid) throws EJDBException;

    /**
     * @param path  BSON field path
     * @param flags
     * @throws EJDBException
     */
    public native void setIndex(String path, int flags) throws EJDBException;

    /**
     * @see EJDBCollection#createQuery(org.bson.BSONObject, org.bson.BSONObject[], org.bson.BSONObject)
     */
    public EJDBQuery createQuery(BSONObject query) {
        return new EJDBQuery(this, query, null, null);
    }

    /**
     * @see EJDBCollection#createQuery(org.bson.BSONObject, org.bson.BSONObject[], org.bson.BSONObject)
     */
    public EJDBQuery createQuery(BSONObject query, BSONObject[] qors) {
        return new EJDBQuery(this, query, qors, null);
    }

    /**
     * @see EJDBCollection#createQuery(org.bson.BSONObject, org.bson.BSONObject[], org.bson.BSONObject)
     */
    public EJDBQuery createQuery(BSONObject query, BSONObject hints) {
        return new EJDBQuery(this, query, null, hints);
    }

    /**
     * Create EJDB query
     * <p/>
     * EJDB queries inspired by MongoDB (mongodb.org) and follows same philosophy.
     *
     * @param query Main BSON query object
     * @param qors  Array of additional OR query objects (joined with OR predicate).
     * @param hints BSON object with query hints.
     * @return
     */
    public EJDBQuery createQuery(BSONObject query, BSONObject[] qors, BSONObject hints) {
        return new EJDBQuery(this, query, qors, hints);
    }

    /**
     * Begin collection transaction.
     *
     * @throws EJDBException
     */
    public void beginTransaction() throws EJDBException {
        this.txControl(JBTXBEGIN);
    }

    /**
     * Commit collection transaction.
     *
     * @throws EJDBException
     */
    public void commitTransaction() throws EJDBException {
        this.txControl(JBTXCOMMIT);
    }

    /**
     * Abort collection transaction.
     *
     * @throws EJDBException
     */
    public void rollbackTransaction() throws EJDBException {
        this.txControl(JBTXROLLBACK);
    }

    /**
     * Get collection transaction status.
     *
     * @throws EJDBException
     */
    public boolean isTransactionActive() throws EJDBException {
        return this.txControl(JBTXSTATUS);
    }

    protected native boolean txControl(int mode) throws EJDBException;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("EJDBCollection");
        sb.append("{cname='").append(cname).append('\'');
        sb.append(", options=").append(options);
        sb.append(", indexes=").append(indexes);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Collection meta information (and creation options)
     */
    public static class Options {
        private long buckets;
        private boolean compressed;
        private boolean large;
        private long records;
        private int cachedRecords;

        public Options() {
        }

        /**
         * @param compressed       If true collection records will be compressed with DEFLATE compression. Default: false.
         * @param large            Specifies that the size of the database can be larger than 2GB. Default: false
         * @param records          Estimated number of records in this collection. Default: 65535.
         * @param cachedRecords    Max number of cached records in shared memory segment. Default: 0
         */
        public Options(boolean compressed, boolean large, long records, int cachedRecords) {
            this.compressed = compressed;
            this.large = large;
            this.records = records;
            this.cachedRecords = cachedRecords;
        }

        public long getBuckets() {
            return buckets;
        }

        public boolean isCompressed() {
            return compressed;
        }

        public boolean isLarge() {
            return large;
        }

        public int getCachedRecords() {
            return cachedRecords;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Options");
            sb.append("{buckets=").append(buckets);
            sb.append(", compressed=").append(compressed);
            sb.append(", large=").append(large);
            sb.append(", cachedRecords=").append(cachedRecords);
            sb.append('}');
            return sb.toString();
        }
    }

    /**
     * Index types
     */
    public static enum IndexType {
        Lexical,
        Numeric,
        Token;
    }

    /**
     * Index meta information
     */
    public static class Index {
        private String name;
        private String field;
        private IndexType type;
        private String file;
        private int records;

        public String getName() {
            return name;
        }

        public String getField() {
            return field;
        }

        public IndexType getType() {
            return type;
        }

        public String getFile() {
            return file;
        }

        public int getRecords() {
            return records;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Index");
            sb.append("{name='").append(name).append('\'');
            sb.append(", field='").append(field).append('\'');
            sb.append(", type=").append(type);
            if (file != null) {
                sb.append(", file='").append(file).append('\'');
            }
            sb.append(", records=").append(records);
            sb.append('}');
            return sb.toString();
        }
    }
}