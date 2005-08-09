/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.reporting.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.*;
import org.apache.log4j.Logger;

public class ReportingSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(ReportingSummarizer.class);

    public ReportingSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {

        long succeededCount = 0;
        long failedCount = 0;

        long c2pOut = 0;
        long p2sOut = 0;
        long s2pOut = 0;
        long p2cOut = 0;
	long numOut = 0;
	
        long c2pIn = 0;
        long p2sIn = 0;
        long s2pIn = 0;
        long p2cIn = 0;
	long numIn = 0;
	
        try {
            String sql = "SELECT sum(c2p_bytes), sum(p2s_bytes), sum(s2p_bytes), sum(p2c_bytes), count(*) FROM pl_endp JOIN pl_stats USING (session_id) WHERE client_intf = 1 AND raze_date >= ? AND create_date < ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            c2pOut = rs.getLong(1);
            p2sOut = rs.getLong(2);
            s2pOut = rs.getLong(3);
            p2cOut = rs.getLong(4);
	    numOut = rs.getLong(5);
            rs.close();
            ps.close();

            sql = "SELECT sum(c2p_bytes), sum(p2s_bytes), sum(s2p_bytes), sum(p2c_bytes), count(*) FROM pl_endp JOIN pl_stats USING (session_id) WHERE client_intf = 0 AND raze_date >= ? AND create_date < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            c2pIn = rs.getLong(1);
            p2sIn = rs.getLong(2);
            s2pIn = rs.getLong(3);
            p2cIn = rs.getLong(4);
	    numIn = rs.getLong(5);
            rs.close();
            ps.close();

            sql = "select count(*) from mvvm_login_evt where time_stamp >= ? and time_stamp < ? and not local and succeeded";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            succeededCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "select count(*) from mvvm_login_evt where time_stamp >= ? and time_stamp < ? and not local and not succeeded";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            failedCount = rs.getLong(1);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        long bytesReceivedFromOutside = s2pOut + c2pIn;
        long bytesSentToOutside = p2sOut + p2cIn;
        long bytesTotalByDirection = bytesReceivedFromOutside + bytesSentToOutside;

        long totalOutboundBytes = p2sOut + p2cOut;
        long totalInboundBytes = c2pIn + s2pIn;

        double numSecs = (endDate.getTime() - startDate.getTime()) / 1000d;


        addEntry("Data transferred", Util.trimNumber("Bytes",bytesTotalByDirection));
        addEntry("&nbsp;&nbsp;&nbsp;Sent to outside", Util.trimNumber("Bytes",bytesSentToOutside) + " (" + Util.percentNumber(bytesSentToOutside,bytesTotalByDirection) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Received from outside", Util.trimNumber("Bytes",bytesReceivedFromOutside) + " (" + Util.percentNumber(bytesReceivedFromOutside,bytesTotalByDirection) + ")");

        addEntry("&nbsp;", "&nbsp;");

        addEntry("Sessions created", Util.trimNumber("",numOut + numIn));
        addEntry("&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber("",numOut) + " (" + Util.percentNumber(numOut,numIn+numOut) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber("",numIn) + " (" + Util.percentNumber(numIn,numIn+numOut) + ")");

	/*        addEntry("&nbsp;", "&nbsp;");

        addEntry("Data sent", "&nbsp;");
        addEntry("&nbsp;&nbsp;&nbsp;During outbound sessions", Util.trimNumber("Bytes",totalOutboundBytes));
        addEntry("&nbsp;&nbsp;&nbsp;During inbound sessions", Util.trimNumber("Bytes",totalInboundBytes));
	*/
        addEntry("&nbsp;", "&nbsp;");

        addEntry("Average data transfer rates", "&nbsp;");
        addEntry("&nbsp;&nbsp;&nbsp;Per second", Util.trimNumber("Bytes/sec",(long) (((double)bytesTotalByDirection) / numSecs)));
        addEntry("&nbsp;&nbsp;&nbsp;Per day", Util.trimNumber( "Bytes/day",(long) (((double)bytesTotalByDirection) / numSecs) * 86400l ));

        addEntry("&nbsp;", "&nbsp;");

        addEntry("Administrative logins", succeededCount+failedCount);
        addEntry("&nbsp;&nbsp;&nbsp;Successful", Util.trimNumber("",succeededCount) + " (" + Util.percentNumber(succeededCount,succeededCount+failedCount)  + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Failed", Util.trimNumber("",failedCount) + " (" +  Util.percentNumber(failedCount,succeededCount+failedCount) + ")");


        return summarizeEntries("Traffic Flow Rates");
    }

}


