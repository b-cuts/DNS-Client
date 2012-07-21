// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A set of Records with the same name, type, and class. Also included are all
 * RRSIG records signing the data records.
 * 
 * @see Record
 * @see RRSIGRecord
 * 
 * @author Brian Wellington
 */

public class RRset implements Serializable {

    private static final long serialVersionUID = -3270249290171239695L;

    /*
     * rrs contains both normal and RRSIG records, with the RRSIG records
     * at the end.
     */
    private List<Record>              rrs;
    private short             nsigs;
    private short             position;

    /** Creates an empty RRset */
    public RRset() {
        rrs = new ArrayList<Record>(1);
        nsigs = 0;
        position = 0;
    }

    /** Creates an RRset and sets its contents to the specified record */
    public RRset(Record record) {
        this();
        safeAddRR(record);
    }

    /** Creates an RRset with the contents of an existing RRset */
    @SuppressWarnings("unchecked")
    public RRset(RRset rrset) {
        synchronized (rrset) {
            rrs = (List<Record>) ((ArrayList<Record>) rrset.rrs).clone();
            nsigs = rrset.nsigs;
            position = rrset.position;
        }
    }

    /** Adds a Record to an RRset */
    public synchronized void addRR(Record r) {
        if (rrs.size() == 0) {
            safeAddRR(r);
            return;
        }
        Record first = first();
        if (!r.sameRRset(first)) {
            throw new IllegalArgumentException("record does not match "
                                               + "rrset");
        }

        if (r.getTTL() != first.getTTL()) {
            if (r.getTTL() > first.getTTL()) {
                r = r.cloneRecord();
                r.setTTL(first.getTTL());
            } else {
                for (int i = 0; i < rrs.size(); i++) {
                    Record tmp = rrs.get(i);
                    tmp = tmp.cloneRecord();
                    tmp.setTTL(r.getTTL());
                    rrs.set(i, tmp);
                }
            }
        }

        if (!rrs.contains(r)) {
            safeAddRR(r);
        }
    }

    /** Deletes all Records from an RRset */
    public synchronized void clear() {
        rrs.clear();
        position = 0;
        nsigs = 0;
    }

    /** Deletes a Record from an RRset */
    public synchronized void deleteRR(Record r) {
        if (rrs.remove(r) && r instanceof RRSIGRecord) {
            nsigs--;
        }
    }

    /**
     * Returns the first record
     * 
     * @throws IllegalStateException
     *             if the rrset is empty
     */
    public synchronized Record first() {
        if (rrs.size() == 0) {
            throw new IllegalStateException("rrset is empty");
        }
        return rrs.get(0);
    }

    /**
     * Returns the class of the records
     * 
     * @see DClass
     */
    public int getDClass() {
        return first().getDClass();
    }

    /**
     * Returns the name of the records
     * 
     * @see Name
     */
    public Name getName() {
        return first().getName();
    }

    /** Returns the ttl of the records */
    public synchronized long getTTL() {
        return first().getTTL();
    }

    /**
     * Returns the type of the records
     * 
     * @see Type
     */
    public int getType() {
        return first().getRRsetType();
    }

    /**
     * Returns an Iterator listing all (data) records. This cycles through the
     * records, so each Iterator will start with a different record.
     */
    public synchronized Iterator<Record> rrs() {
        return iterator(true, true);
    }

    /**
     * Returns an Iterator listing all (data) records.
     * 
     * @param cycle
     *            If true, cycle through the records so that each Iterator will
     *            start with a different record.
     */
    public synchronized Iterator<Record> rrs(boolean cycle) {
        return iterator(true, cycle);
    }

    /** Returns an Iterator listing all signature records */
    public synchronized Iterator<Record> sigs() {
        return iterator(false, false);
    }

    /** Returns the number of (data) records */
    public synchronized int size() {
        return rrs.size() - nsigs;
    }

    /** Converts the RRset to a String */
    @Override
    public String toString() {
        if (rrs == null) {
            return "{empty}";
        }
        StringBuffer sb = new StringBuffer();
        sb.append("{ ");
        sb.append(getName() + " ");
        sb.append(getTTL() + " ");
        sb.append(DClass.string(getDClass()) + " ");
        sb.append(Type.string(getType()) + " ");
        sb.append(iteratorToString(iterator(true, false)));
        if (nsigs > 0) {
            sb.append(" sigs: ");
            sb.append(iteratorToString(iterator(false, false)));
        }
        sb.append(" }");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private synchronized Iterator<Record> iterator(boolean data, boolean cycle) {
        int size, start, total;

        total = rrs.size();

        if (data) {
            size = total - nsigs;
        } else {
            size = nsigs;
        }
        if (size == 0) {
            return (Iterator<Record>) Collections.EMPTY_LIST.iterator();
        }

        if (data) {
            if (!cycle) {
                start = 0;
            } else {
                if (position >= size) {
                    position = 0;
                }
                start = position++;
            }
        } else {
            start = total - nsigs;
        }

        List<Record> list = new ArrayList<Record>(size);
        if (data) {
            list.addAll(rrs.subList(start, size));
            if (start != 0) {
                list.addAll(rrs.subList(0, start));
            }
        } else {
            list.addAll(rrs.subList(start, total));
        }

        return list.iterator();
    }

    private String iteratorToString(Iterator<Record> it) {
        StringBuffer sb = new StringBuffer();
        while (it.hasNext()) {
            Record rr = it.next();
            sb.append("[");
            sb.append(rr.rdataToString());
            sb.append("]");
            if (it.hasNext()) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private void safeAddRR(Record r) {
        if (!(r instanceof RRSIGRecord)) {
            if (nsigs == 0) {
                rrs.add(r);
            } else {
                rrs.add(rrs.size() - nsigs, r);
            }
        } else {
            rrs.add(r);
            nsigs++;
        }
    }

}