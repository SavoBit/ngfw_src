/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.networking;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.Validatable;
import com.untangle.mvvm.tran.ValidateException;
import org.hibernate.annotations.Type;

/**
 * These are settings related to the hostname and the adddress that is
 * used to connect to box.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_address_settings", schema="settings")
public class AddressSettings implements Serializable, Validatable
{
    private static final String PUBLIC_ADDRESS_EXCEPTION =
        "A public address is an ip address, optionally followed by a port.  (e.g. 1.2.3.4:445 or 1.2.3.4)";

    private Long id;
    
    /* boolean which can be used by the untangle to determine if the
     * object returned by a user interface has been modified. */
    private boolean isClean = false;

    /* An additional HTTPS port that the web server will bind to */
    private int httpsPort;

    /* The hostname of the box */
    private HostName hostname;

    /* True if the hostname is resolvable on the internet */
    private boolean isHostnamePublic;

    /* Settings related to having an external router address */
    /* True if this untangle uses a public address */
    private boolean isPublicAddressEnabled;
    
    /* This is the address of an external router that has a redirect
     * to this untangle.  Used when we are in bridge mode behind
     * another box. */
    private IPaddr publicIPaddr;
    
    /* This is the port on the external router that is redirect to the
     * untangle's httpsPort. <code>publicPort</code> is used in
     * conjunction with <code>publicIPaddr</code>
     */
    private int publicPort;

    public AddressSettings()
    {
    }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    Long getId()
    {
        return id;
    }

    void setId( Long id )
    {
        this.id = id;
    }

    /**
     * Get the port to run HTTPs on in addition to port 443. */
    @Column(name="https_port")
    public int getHttpsPort()
    {
        return this.httpsPort;
    }

    /**
     * Set the port to run HTTPs on in addition to port 443. */
    public void setHttpsPort( int newValue )
    {
        if ( this.httpsPort != newValue ) this.isClean = false;
        this.httpsPort = newValue;
    }

    /*
     * Retrieve the hostname for the box (this is the hostname that
     * goes into certificates). */
    @Column(name="hostname")
    @Type(type="com.untangle.mvvm.type.HostNameUserType")
    public HostName getHostName()
    {
        return this.hostname;
    }

    public void setHostName( HostName newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.DEFAULT_HOSTNAME;

        if ( !HostName.equals( this.hostname, newValue )) this.isClean = false;
        this.hostname = newValue;
    }

    /**
     * Returns if the hostname for this box is publicly resolvable to
     * this box */
    @Column(name="is_hostname_public")
    public boolean getIsHostNamePublic()
    {
        return this.isHostnamePublic;
    }

    /**
     * Set if the hostname for this box is publicly resolvable to this
     * box */
    public void setIsHostNamePublic( boolean newValue )
    {
        if ( newValue != this.isHostnamePublic ) this.isClean = false;
        this.isHostnamePublic = newValue;
    }

    /**
     * True if the public address should be used
     */
    @Column(name="has_public_address")
    public boolean getIsPublicAddressEnabled()
    {
        return this.isPublicAddressEnabled;
    }

    public void setIsPublicAddressEnabled( boolean newValue )
    {
        if ( newValue != this.isPublicAddressEnabled ) this.isClean = false;
        this.isPublicAddressEnabled = newValue;
    }

    /**
     * Retrieve the public address for the box.
     *
     * @return the public url for the box, this is the address (may be
     * hostname or ip address)
     */
    @Transient
    public String getPublicAddress()
    {
        if ( this.publicIPaddr == null || this.publicIPaddr.isEmpty()) return "";

        if ( this.publicPort == NetworkUtil.DEF_HTTPS_PORT ) return this.publicIPaddr.toString();

        return this.publicIPaddr.toString() + ":" + this.publicPort;
    }

    /**
     * Set the public address as a string, this is a convenience
     * method for the GUI, it sets the public ip address and port.
     * 
     * @param newValue The hostname and port in a string for the
     * public address and port of the box.  If the port is left off,
     * this used the default https port.
     */
    public void setPublicAddress( String newValue ) throws ParseException
    {
        try {
            IPaddr address;
            String valueArray[] = newValue.split( ":" );
            switch ( valueArray.length ) {
            case 1:
                address = IPaddr.parse( valueArray[0] );
                setPublicIPaddr( address );
                setPublicPort( NetworkUtil.DEF_HTTPS_PORT );
                break;

            case 2:
                address = IPaddr.parse( valueArray[0] );
                int port = Integer.parseInt( valueArray[1] );
                setPublicIPaddr( address );
                setPublicPort( port );
                break;

            default:
                /* just throw an expception to get out of dodge */
                throw new Exception();
            }
        } catch ( Exception e ) {
            throw new ParseException( PUBLIC_ADDRESS_EXCEPTION );
        }
    }

    /**
     * Retrieve the address portion of the public address.
     */
    @Column(name="public_ip_addr")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getPublicIPaddr()
    {
        return this.publicIPaddr;
    }

    /**
     * Set the address portion of the public address.
     *
     * @param newValue the new address for the public address.
     */
    public void setPublicIPaddr( IPaddr newValue )
    {
        if ( IPaddr.equals( this.publicIPaddr, newValue )) this.isClean = false;
        this.publicIPaddr = newValue;
    }

    /**
     * Retrieve the port component of the public address.
     */
    @Column(name="public_port")
    public int getPublicPort()
    {
        if (( this.publicPort <= 0 ) || ( this.publicPort >= 0xFFFF )) {
            this.publicPort = NetworkUtil.DEF_HTTPS_PORT;
        }

        return this.publicPort;
    }

    /**
     * Set the port component of the public address.
     *
     * @param newValue the new port for the public address.
     */
    public void setPublicPort( int newValue )
    {
        if (( newValue <= 0 ) || ( newValue >= 0xFFFF )) newValue = NetworkUtil.DEF_HTTPS_PORT;

        if ( newValue != this.publicPort ) this.isClean = false;
        this.publicPort = newValue;
    }

    /**
     * Return true if the current settings use a public address
     */
    @Transient
    public boolean hasPublicAddress()
    {
        return (( this.publicIPaddr != null ) &&  !this.publicIPaddr.isEmpty());
    }

    /**
     * Return true iff the settings haven't been modified since the
     * last time <code>isClean( true )</code> was called.
     */
    @Transient
    public boolean isClean()
    {
        return this.isClean;
    }

    /**
     * Clear or set the isClean flag.
     *
     * @param newValue The new value for the isClean flag.
     */
    public void isClean( boolean newValue )
    {
        this.isClean = newValue;
    }

    /**
     * Validate that the settings are free of errors.
     */
    @Transient
    public void validate() throws ValidateException
    {
        /* nothing appears to be necessary here for now */
    }
}

