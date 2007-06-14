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

package com.untangle.uvm.engine;

import com.untangle.uvm.policy.LocalPolicyManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.policy.PolicyConfiguration;
import com.untangle.uvm.policy.PolicyException;
import com.untangle.uvm.policy.PolicyManager;
import com.untangle.uvm.policy.SystemPolicyRule;
import com.untangle.uvm.policy.UserPolicyRule;
import java.util.List;

class RemotePolicyManagerAdaptor implements PolicyManager
{
    private final LocalPolicyManager pm;

    RemotePolicyManagerAdaptor(LocalPolicyManager pm)
    {
        this.pm = pm;
    }

    public Policy[] getPolicies()
    {
        return pm.getPolicies();
    }

    public Policy getDefaultPolicy()
    {
        return pm.getDefaultPolicy();
    }

    public Policy getPolicy(String name)
    {
        return pm.getPolicy(name);
    }

    public void addPolicy(String name, String notes) throws PolicyException
    {
        pm.addPolicy(name, notes);
    }

    public void removePolicy(Policy policy) throws PolicyException
    {
        pm.removePolicy(policy);
    }

    public void setPolicy(Policy rule, String name, String notes)
        throws PolicyException
    {
        pm.setPolicy(rule, name, notes);
    }

    public SystemPolicyRule[] getSystemPolicyRules()
    {
        return pm.getSystemPolicyRules();
    }

    public void setSystemPolicyRule(SystemPolicyRule rule, Policy p,
                                    boolean inbound, String description)
    {
        pm.setSystemPolicyRule(rule, p, inbound, description);
    }

    public UserPolicyRule[] getUserPolicyRules()
    {
        return pm.getUserPolicyRules();
    }

    public void setUserPolicyRules(List rules)
    {
        pm.setUserPolicyRules(rules);
    }

    public PolicyConfiguration getPolicyConfiguration()
    {
        return pm.getPolicyConfiguration();
    }

    public void setPolicyConfiguration(PolicyConfiguration pc)
        throws PolicyException
    {
        pm.setPolicyConfiguration(pc);
    }

    public String productIdentifier()
    {
        return pm.productIdentifier();
    }
}
