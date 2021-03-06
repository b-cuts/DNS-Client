// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.IOException;

/**
 * X.400 mail mapping record.
 * 
 * @author Brian Wellington
 */

public class PXRecord extends Record {

    private static final long serialVersionUID = 1811540008806660667L;

    private Name              map822;
    private Name              mapX400;
    private int               preference;

    /**
     * Creates an PX Record from the given data
     * 
     * @param preference
     *            The preference of this mail address.
     * @param map822
     *            The RFC 822 component of the mail address.
     * @param mapX400
     *            The X.400 component of the mail address.
     */
    public PXRecord(Name name, int dclass, long ttl, int preference,
                    Name map822, Name mapX400) {
        super(name, Type.PX, dclass, ttl);

        this.preference = checkU16("preference", preference);
        this.map822 = checkName("map822", map822);
        this.mapX400 = checkName("mapX400", mapX400);
    }

    PXRecord() {
    }

    /** Gets the RFC 822 component of the mail address. */
    public Name getMap822() {
        return map822;
    }

    /** Gets the X.400 component of the mail address. */
    public Name getMapX400() {
        return mapX400;
    }

    /** Gets the preference of the route. */
    public int getPreference() {
        return preference;
    }

    @Override
    Record getObject() {
        return new PXRecord();
    }

    @Override
    void rdataFromString(Tokenizer st, Name origin) throws IOException {
        preference = st.getUInt16();
        map822 = st.getName(origin);
        mapX400 = st.getName(origin);
    }

    @Override
    void rrFromWire(DNSInput in) throws IOException {
        preference = in.readU16();
        map822 = new Name(in);
        mapX400 = new Name(in);
    }

    /** Converts the PX Record to a String */
    @Override
    String rrToString() {
        StringBuffer sb = new StringBuffer();
        sb.append(preference);
        sb.append(" ");
        sb.append(map822);
        sb.append(" ");
        sb.append(mapX400);
        return sb.toString();
    }

    @Override
    void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeU16(preference);
        map822.toWire(out, null, canonical);
        mapX400.toWire(out, null, canonical);
    }

}
