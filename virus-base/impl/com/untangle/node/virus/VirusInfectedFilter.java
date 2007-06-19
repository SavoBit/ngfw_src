/*
 * $HeadURL$
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

package com.untangle.node.virus;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;

/**
 * Filter for infected virus events.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class VirusInfectedFilter implements SimpleEventFilter<VirusEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("Infected Events");

    private final String httpQuery;
    private final String ftpQuery;
    private final String mailQuery;
    private final String smtpQuery;

    // constructors -----------------------------------------------------------

    VirusInfectedFilter(String vendorName)
    {
        httpQuery = "FROM VirusHttpEvent evt "
            + "WHERE evt.result.clean = false "
            + "AND evt.vendorName = '" + vendorName + "' "
            + "AND evt.requestLine.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp DESC";

        ftpQuery = "FROM VirusLogEvent evt "
            + "WHERE evt.result.clean = false "
            + "AND evt.vendorName = '" + vendorName + "' "
            + "AND evt.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp DESC";

        mailQuery = "FROM VirusMailEvent evt "
            + "WHERE evt.result.clean = false "
            + "AND evt.vendorName = '" + vendorName + "' "
            + "AND evt.messageInfo.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp DESC";

        smtpQuery = "FROM VirusSmtpEvent evt "
            + "WHERE evt.result.clean = false "
            + "AND evt.vendorName = '" + vendorName + "' "
            + "AND evt.messageInfo.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp DESC";
    }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { httpQuery, ftpQuery, mailQuery, smtpQuery };
    }

    public boolean accept(VirusEvent e)
    {
        return e.isInfected();
    }
}
