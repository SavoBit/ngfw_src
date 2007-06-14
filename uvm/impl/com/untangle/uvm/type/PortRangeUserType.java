/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.untangle.uvm.node.PortRange;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

public class PortRangeUserType implements CompositeUserType
{
    public Class returnedClass() { return PortRange.class; }

    public boolean equals(Object x, Object y)
    {
        if (x == y) { return true; }
        if (null == x || null == y) { return false; }
        return x.equals(y);
    }

    public Object deepCopy(Object value) { return value; }

    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names,
                              SessionImplementor si, Object owner)
        throws HibernateException, SQLException
    {
        if (rs.wasNull()) { return null; }
        int low = rs.getInt(names[0]);
        int high = rs.getInt(names[1]);

        return new PortRange(low, high);
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i,
                            SessionImplementor si)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.NUMERIC);
            ps.setNull(++i, Types.NUMERIC);
        } else {
            PortRange pr = (PortRange)v;
            ps.setInt(i, pr.getLow());
            ps.setInt(++i, pr.getHigh());
        }
    }

    public String[] getPropertyNames()
    {
        return new String[] { "low", "high" };
    }

    public Type[] getPropertyTypes()
    {
        return new Type[] { Hibernate.INTEGER, Hibernate.INTEGER };
    }

    public Object getPropertyValue(Object o, int i)
        throws HibernateException
    {
        PortRange pr = (PortRange)o;
        switch (i) {
        case 0: return new Integer(pr.getLow());
        case 1: return new Integer(pr.getHigh());
        default: throw new IllegalArgumentException("bad index: " + i);
        }
    }

    public void setPropertyValue(Object o, int i, Object v)
        throws HibernateException
    {
        throw new UnsupportedOperationException("immutable");
    }

    public Object assemble(Serializable cached, SessionImplementor session,
                           Object owner)
        throws HibernateException
    {
        return cached;
    }

    public Serializable disassemble(Object v, SessionImplementor session)
        throws HibernateException
    {
        return (Serializable)v;
    }

    public Object replace(Object original, Object target,
                          SessionImplementor session, Object owner)
    {
        return original;
    }

    public int hashCode(Object x)
    {
        return x.hashCode();
    }
}
