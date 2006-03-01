/*
 * Copyright (c) 2003, 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.mvvm.networking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

import java.net.InetAddress;
import java.net.Inet4Address;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkManager;
import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.HostNameList;
import com.metavize.mvvm.tran.IPNullAddr;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.firewall.MACAddress;

import com.metavize.mvvm.networking.internal.DhcpLeaseInternal;
import com.metavize.mvvm.networking.internal.DnsStaticHostInternal;
import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.metavize.mvvm.networking.internal.NetworkSpaceInternal;
import com.metavize.mvvm.networking.internal.ServicesInternalSettings;

class DhcpManager
{
    private static final String COMMENT = "#";
    private static final String HEADER  = COMMENT + " AUTOGENERATED BY METAVIZE DO NOT MODIFY MANUALLY\n";

    private static final String FLAG_DHCP_RANGE       = "dhcp-range";
    private static final String FLAG_DHCP_HOST        = "dhcp-host";
    private static final String FLAG_DHCP_OPTION      = "dhcp-option";
    private static final String FLAG_DNS_LOCAL_DOMAIN = "domain";
    private static final String FLAG_DNS_LOCALIZE     = "localise-queries";

    private static final String FLAG_DHCP_GATEWAY     = "3";
    private static final String FLAG_DHCP_NETMASK     = "1";
    private static final String FLAG_DHCP_NAMESERVERS = "6";
    private static final String FLAG_DNS_LISTEN       = "listen-address";
    private static final String FLAG_DNS_LISTEN_PORT  = "port";

    private static final String FLAG_DNS_BIND_INTERFACES = "bind-interfaces";
    private static final String FLAG_DNS_INTERFACE       = "interface";

    private static final int DHCP_LEASE_ENTRY_LENGTH  = 5;
    private static final String DHCP_LEASE_DELIM      = " ";

    private static final String DHCP_LEASES_FILE      = "/var/lib/misc/dnsmasq.leases";
    private static final int DHCP_LEASE_ENTRY_EOL     = 0;
    private static final int DHCP_LEASE_ENTRY_MAC     = 1;
    private static final int DHCP_LEASE_ENTRY_IP      = 2;
    private static final int DHCP_LEASE_ENTRY_HOST    = 3;

    private static final String DNS_MASQ_FILE         = "/etc/dnsmasq.conf";
    private static final String DNS_MASQ_CMD          = "/etc/init.d/dnsmasq ";
    private static final String DNS_MASQ_CMD_RESTART  = DNS_MASQ_CMD + " restart";
    private static final String DNS_MASQ_CMD_STOP     = DNS_MASQ_CMD + " stop";

    private static final String HOST_FILE             = "/etc/hosts";
    private static final String[] HOST_FILE_START     = new String[] {
        HEADER,
        "127.0.0.1  localhost"
    };

    private static final String[] HOST_FILE_END       = new String[] {
        "# The following lines are desirable for IPv6 capable hosts",
        "# (added automatically by netbase upgrade)",
        "",
        "::1     ip6-localhost ip6-loopback",
        "fe00::0 ip6-localnet",
        "ff00::0 ip6-mcastprefix",
        "ff02::1 ip6-allnodes",
        "ff02::2 ip6-allrouters",
        "ff02::3 ip6-allhosts"
    };

    private final Logger logger = Logger.getLogger( DhcpManager.class );

    DhcpManager()
    {
    }

    void configure( ServicesInternalSettings settings ) throws NetworkException
    {
        NetworkManagerImpl nm = (NetworkManagerImpl)MvvmContextFactory.context().networkManager();
        
        if ( !settings.getIsEnabled()) {
            logger.debug( "Services are currently disabled, deconfiguring dns masq" );
            deconfigure();
            return;
        }

        try {
            writeConfiguration( settings );
            writeHosts( settings, nm.getNetworkInternalSettings());
        } catch ( Exception e ) {
            throw new NetworkException( "Unable to reload DNS masq configuration", e );
        }

        /* Enable/Disable DHCP forwarding  */
        try {
            if ( settings.getIsDhcpEnabled()) {
                nm.disableDhcpForwarding();
            } else {
                nm.enableDhcpForwarding();
            }
        } catch ( Exception e ) {
            throw new NetworkException( "Error updating DHCP forwarding settings", e );
        }
    }

    void startDnsMasq() throws NetworkException
    {
        int code;

        try {
            logger.debug( "Restarting DNS Masq server" );

            /* restart dnsmasq */
            Process p = Runtime.getRuntime().exec( DNS_MASQ_CMD_RESTART );
            code = p.waitFor();
        } catch ( Exception e ) {
            throw new NetworkException( "Unable to reload Start DNS masq server", e );
        }

        if ( code != 0 ) {
            throw new NetworkException( "Error starting DNS masq server: " + code );
        }
    }

    void deconfigure()
    {
        int code;

        try {
            writeDisabledConfiguration();

            Process p = Runtime.getRuntime().exec( DNS_MASQ_CMD_RESTART );
            code = p.waitFor();

            if ( code != 0 ) logger.error( "Error stopping DNS masq server, returned code: " + code );
        } catch ( Exception e ) {
            logger.error( "Error while disabling the DNS masq server", e );
        }

        /* Re-enable DHCP forwarding */
        try {
            logger.info( "Reenabling DHCP forwarding" );
            MvvmContextFactory.context().networkManager().enableDhcpForwarding();
        } catch ( Exception e ) {
            logger.error( "Error enabling DHCP forwarding", e );
        }
    }

    void loadLeases( DhcpServerSettings settings )
    {
        BufferedReader in = null;

        /* Insert the rules from the leases file, than layover the rules from the settings */
        List<DhcpLeaseRule> leaseList  = new LinkedList<DhcpLeaseRule>();
        Map<MACAddress,Integer> macMap = new HashMap<MACAddress,Integer>();

        /* The time right now to determine if leases have been expired */
        Date now = new Date();

        /* Open up the interfaces file */
        try {
            in = new BufferedReader(new FileReader( DHCP_LEASES_FILE ));
            String str;
            while (( str = in.readLine()) != null ) {
                parseLease( str, leaseList, now, macMap );
            }
        } catch ( FileNotFoundException ex ) {
            logger.info( "The file: " + DHCP_LEASES_FILE + " does not exist yet" );
        } catch ( Exception ex ) {
            logger.error( "Error reading file: " + DHCP_LEASES_FILE, ex );
        } finally {
            try {
                if ( in != null )  in.close();
            } catch ( Exception ex ) {
                logger.error( "Unable to close file: " + DHCP_LEASES_FILE, ex );
            }
        }

        /* Lay over the settings from NAT */
        List <DhcpLeaseRule> staticList = settings.getDhcpLeaseList();
        
        overlayStaticLeases( staticList, leaseList, macMap );

        /* Set the list */
        settings.setDhcpLeaseList( leaseList );
    }

    void parseLease( String str, List<DhcpLeaseRule> leaseList, Date now, Map<MACAddress,Integer> macMap )
    {
        str = str.trim();
        String strArray[] = str.split( DHCP_LEASE_DELIM );
        String tmp, host;
        Date eol;
        MACAddress mac;
        IPNullAddr ip;

        if ( strArray.length != DHCP_LEASE_ENTRY_LENGTH ) {
            logger.error( "Invalid DHCP lease: " + str );
            return;
        }

        tmp  = strArray[DHCP_LEASE_ENTRY_EOL];
        try {
            eol = new Date( Long.parseLong( tmp ) * 1000 );
        } catch ( Exception e ) {
            logger.error( "Invalid DHCP date: " + tmp );
            return;
        }

        if ( eol.before( now )) {
            if (logger.isDebugEnabled()) {
                logger.debug( "Lease already expired: " + str );
            }
            return;
        }

        tmp  = strArray[DHCP_LEASE_ENTRY_MAC];
        try {
            mac = MACAddress.parse( tmp );
        } catch ( Exception e ) {
            logger.error( "Invalid MAC address: " + tmp );
            return;
        }

        tmp  = strArray[DHCP_LEASE_ENTRY_IP];
        try {
            ip = IPNullAddr.parse( tmp );
        } catch ( Exception e ) {
            logger.error( "Invalid IP address: " + tmp );
            return;
        }

        host  = strArray[DHCP_LEASE_ENTRY_HOST];

        /* Insert the lease */
        DhcpLeaseRule rule  = new DhcpLeaseRule( mac, host, ip, IPNullAddr.getNullAddr(), eol, true );

        /* Determine if the rule already exists */
        Integer index = macMap.get( mac );

        if ( index == null ) {
            leaseList.add( rule );
            macMap.put( mac, leaseList.size() - 1 );
        } else {
            /* XXX Right now resolve by MAC is always true */
            leaseList.set( index, rule );
        }
    }

    private void overlayStaticLeases( List<DhcpLeaseRule> staticList, List<DhcpLeaseRule> leaseList,
                                      Map<MACAddress,Integer> macMap )
    {
        if ( staticList == null ) {
            return;
        }

        for ( Iterator<DhcpLeaseRule> iter = staticList.iterator() ; iter.hasNext() ; ) {
            DhcpLeaseRule rule = iter.next();

            MACAddress mac = rule.getMacAddress();
            Integer index = macMap.get( mac );
            if ( index == null ) {
                /* Insert a new rule */
                DhcpLeaseRule currentRule = new DhcpLeaseRule( mac, "", IPNullAddr.getNullAddr(),
                                                               rule.getStaticAddress(), null, true );
                currentRule.setDescription( rule.getDescription());
                currentRule.setCategory( rule.getCategory());

                leaseList.add( currentRule );

                macMap.put( mac, leaseList.size() - 1 );
            } else {
                DhcpLeaseRule currentRule = leaseList.get( index );
                currentRule.setStaticAddress( rule.getStaticAddress());
                currentRule.setResolvedByMac( rule.getResolvedByMac());
                currentRule.setDescription( rule.getDescription());
                currentRule.setCategory( rule.getCategory());
            }
        }
    }

    /* This removes all of the non-static leases */
    void fleeceLeases( DhcpServerSettings settings )
    {
        /* Lay over the settings from NAT */
        List <DhcpLeaseRule> staticList = settings.getDhcpLeaseList();

        for ( Iterator<DhcpLeaseRule> iter = staticList.iterator() ; iter.hasNext() ; ) {
            DhcpLeaseRule rule = iter.next();

            IPNullAddr staticAddress = rule.getStaticAddress();

            if ( staticAddress == null || staticAddress.isEmpty()) {
                iter.remove();
            }
        }
    }


    ServicesSettings updateDhcpRange( ServicesInternalSettings services, IPaddr address, IPaddr netmask )
    {
        ServicesSettings servicesSettings = services.toSettings();

        if ( address == null || netmask == null || address.isEmpty() || netmask.isEmpty()) {
            logger.info( "empty address or netmask, continuing." );
            return servicesSettings;
        }
        
        /* Convert to an array of bytes, to calculate the start/end address */
        byte addressArray[] = address.getAddr().getAddress();
        byte netmaskArray[] = netmask.getAddr().getAddress();
        
        if (( netmaskArray[3] & 0x3F ) != 0 ) {
            logger.info( "Netmask is too restricting, ignoring settings change" );
            return servicesSettings;
        }
        
        byte startArray[] = new byte[4];
        byte endArray[] = new byte[4];
        System.arraycopy( addressArray, 0, startArray, 0, NetworkUtilPriv.IP_ADDR_SIZE_BYTES );
        System.arraycopy( addressArray, 0, endArray, 0, NetworkUtilPriv.IP_ADDR_SIZE_BYTES );
        
        /* The ideal case */
        if ( netmaskArray[3] == 0 ) {
            if (( addressArray[3] < 100 ) || ( addressArray[3] > 200 )) {
                startArray[3] = 100;
                endArray[3]   = (byte)200;
            } else {
                startArray[3] = 16;
                endArray[3]   = 99;
            }
        } else {
            int min = byteToInt( netmaskArray[3] & addressArray[3] );
            int max = byteToInt( min | ( ~netmaskArray[3] ));
                        
            int startValue = byteToInt( addressArray[3] );

            /* Address is in the first half */
            if ( startValue < (( min + max ) / 2 ))  startValue = (( min + max ) / 2 ) + 4;
            else                                     startValue = min + 4;

            int endValue = startValue + (( max - min ) / 2 ) - 16;
            System.out.println( "startValue: " + startValue + " endValue " + endValue );
            startArray[3] = (byte)startValue;
            endArray[3]   = (byte)endValue;
        }
        
        try {
            IPaddr start = new IPaddr((Inet4Address)InetAddress.getByAddress( startArray ));
            IPaddr end   = new IPaddr((Inet4Address)InetAddress.getByAddress( endArray ));
            servicesSettings.setDhcpStartAndEndAddress( start, end );
        } catch ( Exception e ) {
            logger.warn( "Exception creating IP addr, ignoring settings change", e );
        }
        
        return servicesSettings;
    }
    
    private int byteToInt ( byte val ) 
    {
        int num = val;
        if ( num < 0 ) num = num & 0x7F + 0x80;
        return num;
    }
    
    private int byteToInt ( int val ) 
    {
        int num = val;
        
        if ( num < 0 ) num = num & 0x7F + 0x80;
        return num;
    }

    private void writeConfiguration( ServicesInternalSettings settings )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( HEADER );

        if ( settings.getIsDhcpEnabled()) {
            /* XXX Presently always defaulting lease times to a fixed value */
            comment( sb, "DHCP Range:" );
            sb.append( FLAG_DHCP_RANGE + "=" + settings.getDhcpStartAddress().toString());
            sb.append( "," + settings.getDhcpEndAddress().toString() + ",4h\n\n\n" );


            /* XXXX Could move this outside of the is dhcp enabled, which would bind to the
             * inside interface if using NAT, without DHCP but with DNS forwarding
             */
            /* Bind to the inside interface if using Nat */
            String intfName = settings.getInterfaceName();
            // This is handled by the iptables rules.
            // if ( intfName != null ) {
            // comment( sb, "Bind to the inside interface" );
            // sb.append( FLAG_DNS_BIND_INTERFACES + "\n" );
            // sb.append( FLAG_DNS_INTERFACE + "=" + intfName + "\n\n" );
            // }

            /* Configure all of the hosts */
            List<DhcpLeaseInternal> list = settings.getDhcpLeaseList();

            if ( list != null ) {
                for ( DhcpLeaseInternal lease : list ) {
                    if ( !lease.getStaticAddress().isEmpty()) {
                        comment( sb, "Static DHCP Host" );
                        if ( lease.getResolvedByMac()) {
                            sb.append( FLAG_DHCP_HOST + "=" + lease.getMacAddress().toString());
                            sb.append( "," + lease.getStaticAddress().toString() + ",24h\n\n" );
                        } else {
                            sb.append( FLAG_DHCP_HOST + "=" + lease.getHostname());
                            sb.append( "," + lease.getStaticAddress().toString() + ",24h\n\n" );
                        }
                    }
                }
            }

            comment( sb, "Setting the gateway" );
            sb.append( FLAG_DHCP_OPTION + "=" + FLAG_DHCP_GATEWAY );
            sb.append( "," + settings.getDefaultRoute() + "\n\n" );

            comment( sb, "Setting the netmask" );
            sb.append( FLAG_DHCP_OPTION + "=" + FLAG_DHCP_NETMASK );
            sb.append( "," + settings.getNetmask() + "\n\n" );


            appendNameServers( sb, settings );
        } else {
            comment( sb, "DHCP is disabled, not using a range or any host rules\n" );
        }

        if ( !settings.getIsDnsEnabled()) {
            /* Cannot bind to localhost, because that will also disable DHCP */
            comment( sb, "DNS is disabled, binding to port 54" );
            sb.append( FLAG_DNS_LISTEN_PORT + "=54\n\n" );
        } else {
            HostName localDomain = settings.getDnsLocalDomain();
            /* Write out the localdomain */
            comment( sb, "Setting the Local domain name" );

            if ( localDomain.isEmpty()) {
                comment( sb, "Local domain name is empty, using " + NetworkUtil.LOCAL_DOMAIN_DEFAULT );
                localDomain = NetworkUtil.LOCAL_DOMAIN_DEFAULT;
            }
            
            /* This flag is used to return address based on the subnet */
            sb.append( FLAG_DNS_LOCALIZE + "\n" );
            
            sb.append( FLAG_DNS_LOCAL_DOMAIN + "=" + localDomain + "\n\n" );
        }

        writeFile( sb, DNS_MASQ_FILE );
    }

    /**
     * Save the file /etc/hosts
     */
    private void writeHosts( ServicesInternalSettings settings, NetworkSpacesInternalSettings nsis )
    {
        StringBuilder sb = new StringBuilder();

        for ( int c = 0 ; c < HOST_FILE_START.length ; c ++ ) {
            sb.append( HOST_FILE_START[c] + "\n" );
        }

        String hostname = NetworkUtilPriv.getPrivInstance().loadHostname();

        /* Add both the unqualified and the qualified domain */
        if ( hostname.indexOf( "." ) > 0 ) {
            String unqualified = hostname.substring( 0, hostname.indexOf( "." ));
            hostname = unqualified + " " + hostname;
        }

        IPaddr servicesAddress = null;

        if ( nsis != null ) {
            List<NetworkSpaceInternal> networkSpaceList = nsis.getNetworkSpaceList();
            if ( networkSpaceList.size() > 0 ) {
                servicesAddress = networkSpaceList.get( 0 ).getPrimaryAddress().getNetwork();
            }
        }
        
        if ( servicesAddress != null && !servicesAddress.isEmpty()) {
            sb.append( servicesAddress.toString() + "\t" + hostname + "\n" );
        } else {
            // Having 127.0.0.1 is problematic because dnsmasq will serve 127.0.0.1
            // and the server adddress as its address.
            // the address
            sb.append( "127.0.0.1\t" + hostname + "\n" );
        }
        sb.append( "\n" );

        if ( settings.getIsDnsEnabled()) {
            List<DnsStaticHostInternal> hostList = mergeHosts( settings );

            for ( DnsStaticHostInternal host : hostList ) {
                HostNameList hostNameList = host.getHostNameList();
                if ( hostNameList.isEmpty()) {
                    comment( sb, "Empty host name list for host " + host.getStaticAddress().toString());
                } else {
                    sb.append( host.getStaticAddress().toString() + "\t" + hostNameList.toString() + "\n" );
                }
            }
        } else {
            comment( sb, "DNS is disabled, skipping hosts" );
        }

        sb.append( "\n" );

        for ( int c = 0 ; c < HOST_FILE_END.length ; c ++ ) {
            sb.append( HOST_FILE_END[c] + "\n" );
        }

        writeFile( sb, HOST_FILE );
    }

    /**
     * Create a new list will all of entries for the same host in the same list
     */
    private List<DnsStaticHostInternal> mergeHosts( ServicesInternalSettings settings )
    {
        List<DnsStaticHostInternal> list = new LinkedList<DnsStaticHostInternal>();
        Map<IPaddr,DnsStaticHostInternal> map = new HashMap<IPaddr,DnsStaticHostInternal>();

        for ( DnsStaticHostInternal host : settings.getDnsStaticHostList()) {
            IPaddr addr  = host.getStaticAddress();
            DnsStaticHostInternal current = map.get( addr );

            if ( current == null ) {
                /* Make a copy of the static route host */
                current = new DnsStaticHostInternal( new HostNameList( host.getHostNameList()), addr );
                map.put( addr, current );
                list.add( current );
            } else {
                current.getHostNameList().merge( host.getHostNameList());
            }
        }

        /* The settings object guarantees this, but just in case, this is done here as well */
        HostName localDomain = settings.getDnsLocalDomain();
        localDomain = ( localDomain.isEmpty()) ? NetworkUtil.LOCAL_DOMAIN_DEFAULT : localDomain;

        for ( Iterator<DnsStaticHostInternal> iter = list.iterator() ; iter.hasNext() ; ) {
            HostNameList hostNameList = iter.next().getHostNameList();
            hostNameList.qualify( localDomain );
            hostNameList.removeReserved();
        }

        return list;
    }

    /* XXX This should go into a global util class */
    private void writeFile( StringBuilder sb, String fileName )
    {
        BufferedWriter out = null;

        /* Open up the interfaces file */
        try {
            String data = sb.toString();

            out = new BufferedWriter(new FileWriter( fileName ));
            out.write( data, 0, data.length());
        } catch ( Exception ex ) {
            /* XXX May need to catch this exception, restore defaults
             * then try again */
            logger.error( "Error writing file " + fileName + ":", ex );
        }

        try {
            if ( out != null )
                out.close();
        } catch ( Exception ex ) {
            logger.error( "Unable to close file: " + fileName , ex );
        }
    }

    private void appendNameServers( StringBuilder sb, ServicesInternalSettings settings )
    {
        String nameservers = "";
        IPaddr tmp;

        for ( IPaddr dns : settings.getDnsServerList()) nameservers += dns + " ";

        nameservers = nameservers.trim();

        if ( nameservers.length() == 0 ) {
            comment( sb, "No nameservers specified\n" );
        } else {
            comment( sb, "Nameservers:" );
            sb.append( FLAG_DHCP_OPTION + "=" + FLAG_DHCP_NAMESERVERS );
            sb.append( "," + nameservers + "\n\n" );
        }
    }

    /* This guarantees the comment appears with a newline at the end */
    private void comment( StringBuilder sb, String comment )
    {
        sb.append( COMMENT + " " + comment + "\n" );
    }

    private void writeDisabledConfiguration()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( HEADER );
        comment( sb, "DNS is disabled, binding DNS to local host" );
        sb.append( FLAG_DNS_LISTEN + "=" + "127.0.0.1\n\n" );

        writeFile( sb, DNS_MASQ_FILE );
    }
}
