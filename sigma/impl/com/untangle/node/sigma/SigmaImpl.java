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
package com.untangle.node.sigma;


import com.untangle.uvm.tapi.AbstractNode;
import com.untangle.uvm.tapi.Affinity;
import com.untangle.uvm.tapi.Fitting;
import com.untangle.uvm.tapi.PipeSpec;
import com.untangle.uvm.tapi.SoloPipeSpec;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class SigmaImpl extends AbstractNode implements Sigma
{
    private final EventHandler handler = new EventHandler(this);
    private final SoloPipeSpec pipeSpec =
        new SoloPipeSpec("sigma", this, handler, Fitting.OCTET_STREAM,Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };
    private final Logger logger = Logger.getLogger(SigmaImpl.class);

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    public Object getSettings()
    {
        return null;
    }

    public void setSettings(Object settings) { }
}
