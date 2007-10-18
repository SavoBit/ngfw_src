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


package com.untangle.node.virus.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.node.virus.*;
import com.untangle.uvm.*;
import com.untangle.uvm.node.*;

public class FTPConfigJPanel extends MEditTableJPanel {

    public FTPConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("FTP virus scan rules");
        super.setDetailsTitle("rule notes");

        // create actual table model
        FTPTableModel ftpTableModel = new FTPTableModel();
        this.setTableModel( ftpTableModel );
        this.setAddRemoveEnabled(false);
    }
}


class FTPTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 55;  /* block */
    private static final int C3_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW), 120); /* description */

    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, false, true,  false, false, Boolean.class, null, sc.bold("scan"));
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  4, 10,    false, false, true,  false, VirusConfig.class, null, "" );
        return tableColumnModel;
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector,
                                 boolean validateOnly)
        throws Exception {
        for( Vector rowVector : tableVector ){

            VirusConfig virusConfig = (VirusConfig) rowVector.elementAt(4);
            virusConfig.setScan( (Boolean) rowVector.elementAt(2) );
            virusConfig.setNotes( (String) rowVector.elementAt(3) );

            // SAVE SETTINGS ///////
            if( !validateOnly ){
                VirusSettings virusSettings = (VirusSettings) settings;
                virusSettings.setFtpConfig(virusConfig);
            }

        }
    }

    public Vector<Vector> generateRows(Object settings){
        VirusSettings virusSettings = (VirusSettings) settings;
        Vector<Vector> allRows = new Vector<Vector>(2);
        int rowIndex = 0;

        rowIndex++;
        Vector row = new Vector(6);
        VirusConfig virusCtl  = virusSettings.getFtpConfig();
        row.add( super.ROW_SAVED );
        row.add( rowIndex );
        row.add( virusCtl.getScan() );
        row.add( virusCtl.getNotes() );
        row.add( virusCtl );
        allRows.add(row);

        return allRows;
    }
}
