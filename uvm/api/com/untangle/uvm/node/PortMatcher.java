/**
 * $HeadURL$
 */
package com.untangle.uvm.node;

import java.util.LinkedList;

import org.apache.log4j.Logger;

/**
 * This class manages the "matching" of ports.
 *
 * PortMatchers are a string that matches ports
 * "80" matches port 80
 * "81,82" matches port 81 and 82
 * "80-90" matches port 80 through port 90
 * "80,90-100" matches 80 and 90-100
 * "any" matches any interface
 * "none" matches nothing
 */
@SuppressWarnings("serial")
public class PortMatcher implements java.io.Serializable
{
    private static final String MARKER_ANY = "any";
    private static final String MARKER_ALL = "all";
    private static final String MARKER_NONE = "none";
    private static final String MARKER_SEPERATOR = ",";
    private static final String MARKER_RANGE = "-";

    private static PortMatcher ANY_MATCHER = new PortMatcher(MARKER_ANY);
     
    private final Logger logger = Logger.getLogger(getClass());

    
    public String matcher;

    private enum PortMatcherType { ANY, NONE, SINGLE, RANGE, LIST };
    
    /**
     * The type of this matcher
     */
    private PortMatcherType type = PortMatcherType.NONE;

    /**
     * if this port matcher is a list of port matchers, this list stores the children
     */
    private LinkedList<PortMatcher> children = null;

    /**
     * if its a range these two variable store the min and max
     */
    private int rangeMin = -1;
    private int rangeMax = -1;

    /**
     * if its just an int matcher this stores the number
     */
    private int singleInt = -1;


    public PortMatcher(String matcher)
    {
        initialize(matcher);
    }

    public String getMatcher()
    {
        return this.matcher;
    }

    public void setMatcher( String matcher )
    {
        initialize(matcher);
    }
    
    /**
     * Return true if <param>port</param> matches this matcher.
     *
     * @param port The port to test
     * @return True if the <param>port</param> matches.
     */
    public boolean isMatch( int port )
    {
        switch (this.type) {

        case ANY:
            return true;

        case NONE:
            return false;

        case SINGLE:
            if (singleInt == port)
                return true;
            return false;

        case RANGE:
            if (port >= rangeMin && port <= rangeMax)
                return true;
            return false;

        case LIST:
            for (PortMatcher child : this.children) {
                if (child.isMatch(port))
                    return true;
            }
            return false;

        default:
            logger.warn("Unknown port matcher type: " + this.type);
            return false;
        }
        
        
    }

    /**
     * return string representation
     */
    public String toString()
    {
        return matcher;
    }

    public static synchronized PortMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }
    
    /**
     * Initialize all the private variables
     */
    private void initialize( String matcher )
    {
        matcher = matcher.toLowerCase().trim().replaceAll("\\s","");
        this.matcher = matcher;

        /**
         * If it contains a comma it must be a list of port matchers
         * if so, go ahead and initialize the children
         */
        if (matcher.contains(MARKER_SEPERATOR)) {
            this.type = PortMatcherType.LIST;

            this.children = new LinkedList<PortMatcher>();

            String[] results = matcher.split(MARKER_SEPERATOR);
            
            /* check each one */
            for (String childString : results) {
                PortMatcher child = new PortMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * Check the common constants
         */
        if (MARKER_ANY.equals(matcher))  {
            this.type = PortMatcherType.ANY;
            return;
        }
        if (MARKER_ALL.equals(matcher)) {
            this.type = PortMatcherType.ANY;
            return;
        }
        if (MARKER_NONE.equals(matcher)) {
            this.type = PortMatcherType.NONE;
            return;
        }
        
        /**
         * If it contains a dash it must be a range
         */
        if (matcher.contains(MARKER_RANGE)) {
            this.type = PortMatcherType.RANGE;
            
            String[] results = matcher.split(MARKER_RANGE);

            if (results.length != 2) {
                logger.warn("Invalid PortMatcher: Invalid Range: " + matcher);
                throw new java.lang.IllegalArgumentException("Invalid PortMatcher: Invalid Range: " + matcher);
            }

            try {
                this.rangeMin = Integer.parseInt(results[0]);
                this.rangeMax = Integer.parseInt(results[1]);
            } catch (NumberFormatException e) {
                logger.warn("Unknown PortMatcher format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown PortMatcher format: \"" + matcher + "\"", e);
            }

            return;
        }
            
        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = PortMatcherType.SINGLE;
        try {
            this.singleInt = Integer.parseInt(matcher);
        } catch (NumberFormatException e) {
            logger.warn("Unknown PortMatcher format: \"" + matcher + "\"", e);
            throw new java.lang.IllegalArgumentException("Unknown PortMatcher format: \"" + matcher + "\"", e);

        }

        return;
    }
    
}
