/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

/**
 * Token that represents metadata about the stream rather than content.
 */
public class MetadataToken implements Token
{
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    // Token methods ----------------------------------------------------------

    public final ByteBuffer getBytes()
    {
        return EMPTY_BUFFER;
    }

    public int getEstimatedSize()
    {
        return 0;
    }
}
