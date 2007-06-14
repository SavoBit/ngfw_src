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

package com.untangle.node.webfilter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.node.http.RequestLine;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

public class WebFilterAllFilter implements ListEventFilter<WebFilterEvent>
{
    private static final String RL_QUERY = "FROM RequestLine rl ORDER BY rl.httpRequestEvent.timeStamp DESC";
    private static final String EVT_QUERY = "FROM WebFilterEvent evt WHERE evt.requestLine = :requestLine";

    private static final RepositoryDesc REPO_DESC
        = new RepositoryDesc("All HTTP Traffic");

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public boolean accept(WebFilterEvent e)
    {
        return true;
    }

    public void warm(Session s, List<WebFilterEvent> l, int limit,
                     Map<String, Object> params)
    {
        Query q = s.createQuery(RL_QUERY);
        for (String param : q.getNamedParameters()) {
            Object o = params.get(param);
            if (null != o) {
                q.setParameter(param, o);
            }
        }

        q.setMaxResults(limit);

        int c = 0;
        for (Iterator i = q.iterate(); i.hasNext() && ++c < limit; ) {
            RequestLine rl = (RequestLine)i.next();
            Query evtQ = s.createQuery(EVT_QUERY);
            evtQ.setEntity("requestLine", rl);
            WebFilterEvent evt = (WebFilterEvent)evtQ.uniqueResult();
            if (null == evt) {
                evt = new WebFilterEvent(rl, null, null, null, true);
            }
            Hibernate.initialize(evt);
            Hibernate.initialize(evt.getRequestLine());
            l.add(evt);
        }
    }
}
