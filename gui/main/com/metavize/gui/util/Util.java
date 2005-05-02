/*
 * util.java
 *
 * Created on March 28, 2004, 5:52 PM
 */

package com.metavize.gui.util;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.jnlp.*;

import java.net.*;

import com.metavize.gui.login.MLoginJFrame;
import com.metavize.gui.main.MMainJFrame;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.client.*;


/**
 *
 * @author  Ian Nieves
 */
public class Util {

    // VERSION INFO /////////////////
    private static String version = "1.3";
    public static String getVersion(){ return version; }
    /////////////////////////////////
    
    // SERVER PROXIES ///////////////
    private static MvvmContext mvvmContext;
    private static ToolboxManager toolboxManager;
    private static TransformManager transformManager;

    private static StatsCache statsCache;

    public static void setMvvmContext(MvvmContext mvvmContextX){
	mvvmContext = mvvmContextX;
	toolboxManager = mvvmContext.toolboxManager();
	transformManager = mvvmContext.transformManager();

        // Somewhere else this should go? XXX jdi
        statsCache = new StatsCache();
    }
    public static MvvmContext getMvvmContext(){ return mvvmContext; }
    public static ToolboxManager getToolboxManager() { return toolboxManager; }
    public static TransformManager getTransformManager() { return transformManager; }
    public static StatsCache getStatsCache() { return statsCache; }
    ///////////////////////////////////

    // CODEBASE /////////////////
    private static URL serverCodeBase;

    public static URL getServerCodeBase(){
	if(serverCodeBase != null)
	    return serverCodeBase;
	else{
	    try{
		BasicService basicService = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
		serverCodeBase = basicService.getCodeBase();
	    }
	    catch(Exception e){
		Util.handleExceptionNoRestart("Error:", e);
	    }
	    finally{
		return serverCodeBase;
	    }
	}	    
    }

    public static boolean isSecureViaHttps(){
	try{
	    String protocol = getServerCodeBase().getProtocol();
	    if( protocol.equals("https") )
		return true;
	    else
		return false;
	}
	catch(Exception e){
	    return false;
	}
    }
    /////////////////////////////////


    // DefaultTableColumnModel constants /////////
    public static final int TABLE_TOTAL_WIDTH = 471; /* in pixels (contains extra pixel) */
    public static final int LINENO_MIN_WIDTH = 45; /* # */
    public static final int STATUS_MIN_WIDTH = 55; /* status */
    //////////////////////////////////////////////


    private static ClassLoader initClassLoader = null;
    private static MURLClassLoader mURLClassLoader = null;
    private static boolean killThreads = false;
    private static final boolean PRINT_MESSAGES = true;
    private static boolean isDemo = false;
    private static JProgressBar statusJProgressBar;
    private static MPipelineJPanel mPipelineJPanel;
    
    private static MMainJFrame mMainJFrame;
    private static MLoginJFrame mLoginJFrame;
    private static String[] args;

    private static JFrame jFrame;

    private static Vector log;

    static{
        log = new Vector();
    }

    public static boolean getKillThreads(){
        return killThreads;
    }
    public static void exit(int i){
        killThreads = true;
        System.exit(i);
    }

    // EMAIL AND VIRUS AUTO-DISCOVERY ///////////////////
    private static String undetectedMessage  = "<html>To access this functionality, you must install an AntiVirus Scanner Softwawre Appliance.</html>";
    private static EmailDetectionJPanel emailDetectionSophosJPanel;
    private static EmailDetectionJPanel emailDetectionFprotJPanel;
    private static EmailDetectionJPanel emailDetectionHauriJPanel;
    private static EmailDetectionJPanel emailDetectionClamJPanel;
    private static MEditTableJPanel virusMEditTableJPanel;

    public synchronized static void setEmailAndVirusJPanel(String transformName, JPanel jPanel){
	if(transformName == null)
	    return;
	if( transformName.equals("sophos-transform") )
	    emailDetectionSophosJPanel = (EmailDetectionJPanel) jPanel;
	else if( transformName.equals("fprot-transform") )
	    emailDetectionFprotJPanel = (EmailDetectionJPanel) jPanel;
	else if( transformName.equals("hauri-transform") )
	    emailDetectionHauriJPanel = (EmailDetectionJPanel) jPanel;
	else if( transformName.equals("clam-transform") )
	    emailDetectionClamJPanel = (EmailDetectionJPanel) jPanel;
	else if( transformName.equals("email-transform") )
	    virusMEditTableJPanel = (MEditTableJPanel) jPanel;

	SwingUtilities.invokeLater( new Runnable() { public void run() {
	    updateDependencies();
	}});
    }

    private static void updateDependencies(){
	if( virusMEditTableJPanel != null){
	    if(emailDetectionSophosJPanel != null)
		emailDetectionSophosJPanel.setDetected(true);
	    if(emailDetectionFprotJPanel != null)
		emailDetectionFprotJPanel.setDetected(true);
            if(emailDetectionHauriJPanel != null)
		emailDetectionHauriJPanel.setDetected(true);
            if(emailDetectionClamJPanel != null)
		emailDetectionClamJPanel.setDetected(true);
	}
	else{
	    if(emailDetectionSophosJPanel != null)
		emailDetectionSophosJPanel.setDetected(false);
	    if(emailDetectionFprotJPanel != null)
		emailDetectionFprotJPanel.setDetected(false);
            if(emailDetectionHauriJPanel != null)
		emailDetectionHauriJPanel.setDetected(false);
            if(emailDetectionClamJPanel != null)
		emailDetectionClamJPanel.setDetected(false);
	}

	if(virusMEditTableJPanel != null){
	    int index = ((JTabbedPane)virusMEditTableJPanel.getParent()).indexOfComponent(virusMEditTableJPanel);
	    if( emailDetectionSophosJPanel != null ){
		((JTabbedPane)virusMEditTableJPanel.getParent()).setTitleAt(index, "Sophos AntiVirus");
		virusMEditTableJPanel.setMessage( null );
	    }
            else if( emailDetectionFprotJPanel != null ){
		((JTabbedPane)virusMEditTableJPanel.getParent()).setTitleAt(index, "F-Prot AntiVirus");
		virusMEditTableJPanel.setMessage( null );
	    }
            else if( emailDetectionHauriJPanel != null ){
		((JTabbedPane)virusMEditTableJPanel.getParent()).setTitleAt(index, "Hauri AntiVirus");
		virusMEditTableJPanel.setMessage( null );
	    }
	    else if( emailDetectionClamJPanel != null ){
		((JTabbedPane)virusMEditTableJPanel.getParent()).setTitleAt(index, "Clam AntiVirus");
		virusMEditTableJPanel.setMessage( null );
	    }
	    else{
		((JTabbedPane)virusMEditTableJPanel.getParent()).setTitleAt(index, "AntiVirus (uninstalled)");
		virusMEditTableJPanel.setMessage( undetectedMessage );
	    }
	}
    }
    ////////////////////////////////////////////


    public static ClassLoader getInitClassLoader(){ return initClassLoader; }
    public static void setInitClassLoader(ClassLoader initClassLoaderX){ initClassLoader = initClassLoaderX;}

    public static MURLClassLoader getClassLoader(){ return mURLClassLoader; }
    public static void setClassLoader(MURLClassLoader mURLClassLoaderX){ mURLClassLoader = mURLClassLoaderX;}

    public static JProgressBar getStatusJProgressBar(){ return statusJProgressBar; }
    public static void setStatusJProgressBar(JProgressBar statusJProgressBarX){ statusJProgressBar = statusJProgressBarX; }

    public static boolean getIsDemo(){ return isDemo; }
    public static void setIsDemo(boolean isDemoX){ isDemo = isDemoX; }

    public static MPipelineJPanel getMPipelineJPanel(){ return mPipelineJPanel; }
    public static void setMPipelineJPanel(MPipelineJPanel mPipelineJPanelX){ mPipelineJPanel = mPipelineJPanelX; }

    public static void setMLoginJFrame(MLoginJFrame mLoginJFrameX){ mLoginJFrame = mLoginJFrameX; }
    public static JFrame getMLoginJFrame(){ return mLoginJFrame; }

    public static void setMMainJFrame(MMainJFrame mMainJFrameX){ mMainJFrame = mMainJFrameX; }
    public static MMainJFrame getMMainJFrame(){ return mMainJFrame; }


    public static void setArgs( String argsX[] ){
        args = argsX;
    }


    public static GraphicsConfiguration getGraphicsConfiguration(){
	GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
        GraphicsConfiguration graphicsConfiguration = graphicsDevice.getDefaultConfiguration();
	return graphicsConfiguration;
    }

    public static Rectangle generateCenteredBounds(Rectangle parentBounds, int childWidth, int childHeight){
        Rectangle childBounds;
        Rectangle defaultScreenBounds;

        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
        defaultScreenBounds = graphicsConfiguration.getBounds();

        if(parentBounds == null){
            parentBounds = defaultScreenBounds;
        }

        int xCenter = parentBounds.x + parentBounds.width/2;
        int yCenter = parentBounds.y + parentBounds.height/2;
        childBounds = new Rectangle( (xCenter-(childWidth/2)),
                                     (yCenter-(childHeight/2)),
                                     childWidth,
                                     childHeight );

        if(childBounds.x < 0)
            childBounds.x = 0;
        if(childBounds.y < 0)
            childBounds.y = 0;

        return childBounds;
    }

    public static String wrapString(String originalString, int lineLength){
	StringTokenizer stringTokenizer = new StringTokenizer(originalString);
	StringBuffer stringBuffer = new StringBuffer();
	String tempString;
	int currentLineLength = 0;
	while( stringTokenizer.hasMoreTokens() ){
	    tempString = stringTokenizer.nextToken();
	
	    if( currentLineLength + tempString.length() >= lineLength ){
		stringBuffer.append("<br>" + tempString + " ");
		currentLineLength = tempString.length() + 1;
	    }
	    else{
		stringBuffer.append(tempString + " ");
		currentLineLength += (tempString.length() + 1);
	    }
	}
	return stringBuffer.toString();
    }

    public static int determineMinHeight(int attemptedMinHeight){
        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
	Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets( graphicsConfiguration );
	int screenHeight = graphicsConfiguration.getBounds().height - screenInsets.top - screenInsets.bottom;
	//	System.err.println("Determined screen height to be: " + screenHeight);
	if( screenHeight < attemptedMinHeight)
	    return screenHeight;
	else
	    return attemptedMinHeight;
    }
    
    public static void resizeCheck(final Component resizableComponent, Dimension minSize, Dimension maxSize){
        
        Dimension currentSize = resizableComponent.getSize();
	/*
	System.err.println("----------------------");
        System.err.println("| Initial size: " + currentSize);
        System.err.println("| Min size: " + minSize);
        System.err.println("| Max size: " + maxSize);
	System.err.println("----------------------");
	*/
        boolean resetSize = false;
        if(currentSize.width < minSize.width){
            currentSize.width = minSize.width;
            resetSize = true;
        }
        else if(currentSize.width > maxSize.width){
            currentSize.width = maxSize.width;
            resetSize = true;
        }
        if(currentSize.height < minSize.height){
            currentSize.height = minSize.height;
            resetSize = true;
        }
        else if(currentSize.height > maxSize.height){
            currentSize.height = maxSize.height;
            resetSize = true;
        }
        if(resetSize){
	    //Rectangle rectangle = resizableComponent.getBounds();
            //resizableComponent.setBounds( rectangle.x, rectangle.y, currentSize.width, currentSize.height);
	    final Dimension newSize = currentSize;
	    SwingUtilities.invokeLater( new Runnable() { public void run(){
		resizableComponent.setSize( newSize );
		//System.err.println(" SCREEN CHANGE ---> New size: " + newSize);
	    }});
        }
        else{
            //System.err.println(" NO SCREEN CHANGE");
        }
        
    }

    public static void handleExceptionNoRestart(String output, Exception e){
        printMessage(output);
        if(PRINT_MESSAGES)
            e.printStackTrace(System.err);
        log.add(e.getMessage());
        log.add(e.getStackTrace());
    }


    public static void handleExceptionWithRestart(String output, Exception e) throws Exception {
        // DEAL WITH COMMUNICATIONS FAILURES
	// System.err.println("catching exception: " + e.toString() );
	Throwable throwableRef = e;
	
	while( throwableRef != null){
	    if( throwableRef instanceof InvocationConnectionException ){
		mLoginJFrame.resetLogin("Server communication failure.  Re-login.");
		if(PRINT_MESSAGES)
		    e.printStackTrace(System.err);
		mLoginJFrame.reshowLogin();
		return;
	    }
	    else if( throwableRef instanceof InvocationTargetExpiredException ){
		mLoginJFrame.resetLogin("Server synchronization failure.  Re-login.");
		if(PRINT_MESSAGES)
		    e.printStackTrace(System.err);
		mLoginJFrame.reshowLogin();
		return;
	    }
	    else if( throwableRef instanceof LoginExpiredException ){
		mLoginJFrame.resetLogin("Login expired.  Re-login.");
		if(PRINT_MESSAGES)
		    e.printStackTrace(System.err);
		mLoginJFrame.reshowLogin();
		return;
	    }
	    else if(    (throwableRef instanceof ConnectException)
		     || (throwableRef instanceof SocketException)
		     || (throwableRef instanceof SocketTimeoutException) ){
		mLoginJFrame.resetLogin("Server connection failure.  Re-login.");
		if(PRINT_MESSAGES)
		    e.printStackTrace(System.err);
		mLoginJFrame.reshowLogin();
		return;
	    }
	    throwableRef = throwableRef.getCause();
	}
        
	// System.err.println("THROWING");
	throw e;
        
    }

    public static void printMessage(String message){
        if(PRINT_MESSAGES)
            System.err.println(message);
    }

    /* choose largest of two values */
    public static int chooseMax(int iValue, int iMinValue)
    {
        if (iValue >= iMinValue)
        {
            return iValue;
        }
        else
        {
            return iMinValue;
        }
    }

    public static boolean isArrayEmpty(Object[] inArray){
	if( inArray == null )
	    return true;
	else if( inArray.length <= 0 )
	    return true;
	else
	    return false;
    }
}
