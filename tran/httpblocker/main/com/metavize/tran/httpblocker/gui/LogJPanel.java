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

package com.metavize.tran.httpblocker.gui;

import java.util.*;
import javax.swing.table.*;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.httpblocker.Action;
import com.metavize.tran.httpblocker.HttpBlocker;
import com.metavize.tran.httpblocker.HttpRequestLog;
import com.metavize.tran.httpblocker.Reason;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);
	setTableModel(new LogTableModel());
    }

    public Vector generateRows(Object settings){

        List<HttpRequestLog> requestLogList = (List<HttpRequestLog>) ((HttpBlocker)super.logTransform).getEvents(depthJSlider.getValue());
        Vector allEvents = new Vector();

        Vector test = new Vector();
        Vector event;

        for( HttpRequestLog requestLog : requestLogList ){
            event = new Vector();
            event.add( Util.getLogDateFormat().format( requestLog.timeStamp() ));

            Action action = requestLog.getAction();
            Reason reason = requestLog.getReason();
	    
	    event.add( action.toString() );
            event.add( requestLog.getClientAddr() + ":" + ((Integer)requestLog.getCClientPort()).toString() );
	    event.add( requestLog.getUrl().toString() );
	    event.add( reason.toString() );
            event.add( requestLog.getDirection().getDirectionName() );
            event.add( requestLog.getServerAddr() + ":" + ((Integer)requestLog.getSServerPort()).toString() );
            allEvents.insertElementAt(event,0);
        }

        return allEvents;
    }



    class LogTableModel extends MSortedTableModel{

    public TableColumnModel getTableColumnModel(){
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min  rsz    edit   remv   desc   typ               def
        addTableColumn( tableColumnModel,  0,  125, true,  false, false, false, String.class, null, "timestamp" );
        addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
        addTableColumn( tableColumnModel,  2,  155, true,  false, false, false, String.class, null, sc.html("client") );
        addTableColumn( tableColumnModel,  3,  200, true,  false, false, false, String.class, null, "request" );
        addTableColumn( tableColumnModel,  4,  140, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
        addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, sc.html("request<br>direction") );
        addTableColumn( tableColumnModel,  6,  155, true,  false, false, false, String.class, null, "server" );

        return tableColumnModel;
    }

    public void generateSettings(Object settings, boolean validateOnly) throws Exception {}

    public Vector generateRows(Object settings) {
        return LogJPanel.this.generateRows(null);
    }

    }

}
