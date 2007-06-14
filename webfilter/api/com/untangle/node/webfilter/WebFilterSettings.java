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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.untangle.uvm.security.Tid;
import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.uvm.node.MimeTypeRule;
import com.untangle.uvm.node.StringRule;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * WebFilter settings.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_webfilter_settings", schema="settings")
public class WebFilterSettings implements Serializable
{
    private static final long serialVersionUID = 1806394002255164868L;

    private Long id;
    private Tid tid;

    private BlockTemplate blockTemplate = new BlockTemplate();

    private boolean blockAllIpHosts = false;

    private boolean fascistMode = false;

    private List<IPMaddrRule> passedClients = new ArrayList<IPMaddrRule>();
    private List<StringRule> passedUrls = new ArrayList<StringRule>();

    private List<StringRule> blockedUrls = new ArrayList<StringRule>();
    private List<MimeTypeRule> blockedMimeTypes = new ArrayList<MimeTypeRule>();
    private List<StringRule> blockedExtensions = new ArrayList<StringRule>();
    private List<BlacklistCategory> blacklistCategories = new ArrayList<BlacklistCategory>();

    // constructors -----------------------------------------------------------

    public WebFilterSettings() { }

    public WebFilterSettings(Tid tid)
    {
        this.tid = tid;
    }

    // business methods ------------------------------------------------------

    public void addBlacklistCategory(BlacklistCategory category)
    {
        blacklistCategories.add(category);
    }

    public BlacklistCategory getBlacklistCategory(String name)
    {
        for (Iterator i = blacklistCategories.iterator(); i.hasNext(); ) {
            BlacklistCategory bc = (BlacklistCategory)i.next();
            if (name.equals(bc.getName())) {
                return bc;
            }
        }

        return null;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Node id for these settings.
     *
     * @return tid for these settings.
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Template for block messages.
     *
     * @return the block message.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="template", nullable=false)
    public BlockTemplate getBlockTemplate()
    {
        return blockTemplate;
    }

    public void setBlockTemplate(BlockTemplate blockTemplate)
    {
        this.blockTemplate = blockTemplate;
    }

    /**
     * Block all requests to hosts identified only by an IP address.
     *
     * @return true when IP requests are blocked.
     */
    @Column(name="block_all_ip_hosts", nullable=false)
    public boolean getBlockAllIpHosts()
    {
        return blockAllIpHosts;
    }

    public void setBlockAllIpHosts(boolean blockAllIpHosts)
    {
        this.blockAllIpHosts = blockAllIpHosts;
    }

    /**
     * If true, block everything that is not whitelisted.
     *
     * @return true to block.
     */
    @Column(name="fascist_mode", nullable=false)
    public boolean getFascistMode()
    {
        return fascistMode;
    }

    public void setFascistMode(boolean fascistMode)
    {
        this.fascistMode = fascistMode;
    }

    /**
     * Clients not subject to blacklisting.
     *
     * @return the list of passed clients.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_webfilter_passed_clients",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<IPMaddrRule> getPassedClients()
    {
        return passedClients;
    }

    public void setPassedClients(List<IPMaddrRule> passedClients)
    {
        this.passedClients = passedClients;
    }

    /**
     * URLs not subject to blacklist checking.
     *
     * @return the list of passed URLs.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_webfilter_passed_urls",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<StringRule> getPassedUrls()
    {
        return passedUrls;
    }

    public void setPassedUrls(List<StringRule> passedUrls)
    {
        this.passedUrls = passedUrls;
    }

    /**
     * URLs not subject to blacklist checking.
     *
     * @return the list of blocked URLs.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_webfilter_blocked_urls",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<StringRule> getBlockedUrls()
    {
        return blockedUrls;
    }

    public void setBlockedUrls(List<StringRule> blockedUrls)
    {
        this.blockedUrls = blockedUrls;
    }

    /**
     * Mime-Types to be blocked.
     *
     * @return the list of blocked MIME types.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_webfilter_mime_types",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<MimeTypeRule> getBlockedMimeTypes()
    {
        return blockedMimeTypes;
    }

    public void setBlockedMimeTypes(List<MimeTypeRule> blockedMimeTypes)
    {
        this.blockedMimeTypes = blockedMimeTypes;
    }

    /**
     * Extensions to be blocked.
     *
     * @return the list of blocked extensions.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_webfilter_extensions",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<StringRule> getBlockedExtensions()
    {
        return blockedExtensions;
    }

    public void setBlockedExtensions(List<StringRule> blockedExtensions)
    {
        this.blockedExtensions = blockedExtensions;
    }

    /**
     * Blacklist blocking options.
     *
     * @return the list of blacklist categories.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="setting_id")
    @IndexColumn(name="position")
    public List<BlacklistCategory> getBlacklistCategories()
    {
        return blacklistCategories;
    }

    public void setBlacklistCategories(List<BlacklistCategory> blacklistCategories)
    {
        this.blacklistCategories = blacklistCategories;
    }
}
