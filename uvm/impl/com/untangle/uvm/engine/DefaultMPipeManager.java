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

import java.net.*;

import com.untangle.uvm.tapi.MPipeManager;


/**
 * Creates this platform's default MPipeManager
 */

class DefaultMPipeManager {
    /**
     * Prevent instantiation.
     */
    private DefaultMPipeManager() { }

    /**
     * Returns the default MPipeManager.
     */
    public static MPipeManager create() {
        // We already verified change-mPipe-manager permission, so don't worry about
        // enforcing singularity here.
        return new MPipeManagerImpl();
    }
}
