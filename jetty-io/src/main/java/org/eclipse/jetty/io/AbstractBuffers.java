//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.io;

import org.eclipse.jetty.io.nio.DirectNIOBuffer;
import org.eclipse.jetty.io.nio.IndirectNIOBuffer;

public abstract class AbstractBuffers implements Buffers
{
    protected Buffers.Type _headerType;
    protected int _headerSize;
    protected Buffers.Type _bufferType;
    protected int _bufferSize;
    protected final Buffers.Type _otherType;

    /* ------------------------------------------------------------ */
    public AbstractBuffers(Buffers.Type headerType, int headerSize, Buffers.Type bufferType, int bufferSize, Buffers.Type otherType)
    {
        _headerType=headerType;
        _headerSize=headerSize;
        _bufferType=bufferType;
        _bufferSize=bufferSize;
        _otherType=otherType;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the buffer size in bytes.
     */
    public int getBufferSize()
    {
        return _bufferSize;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the header size in bytes.
     */
    public int getHeaderSize()
    {
        return _headerSize;
    }


    /* ------------------------------------------------------------ */
    /**
     * Create a new header Buffer
     * @return new Buffer
     */
    final protected Buffer newHeader()
    {
    	Buffer buffer;
        switch(_headerType)
        {
            case BYTE_ARRAY:
            {
            	buffer = new ByteArrayBuffer(_headerSize);
            	buffer.setServiceType(Buffers.ServiceType.HEADER);
            	buffer.setBufferType(_headerType);
            	return buffer;
            }
            case DIRECT:
            {
            	buffer = new DirectNIOBuffer(_headerSize);
            	buffer.setServiceType(Buffers.ServiceType.HEADER);
            	buffer.setBufferType(_headerType);
            	return buffer;
            }
            case INDIRECT:
            {
            	buffer = new IndirectNIOBuffer(_headerSize);
            	buffer.setServiceType(Buffers.ServiceType.HEADER);
            	buffer.setBufferType(_headerType);
            	return buffer;
            }
        }
        throw new IllegalStateException();
    }

    /* ------------------------------------------------------------ */
    /**
     * Create a new content Buffer
     * @return new Buffer
     */
    final protected Buffer newBuffer()
    {
       Buffer buffer;
       switch(_bufferType)
       {
       		case BYTE_ARRAY:
       		{
       			buffer = new ByteArrayBuffer(_bufferSize);
       			buffer.setServiceType(Buffers.ServiceType.BODY);
       			buffer.setBufferType(_bufferType);
       			return buffer;
       		}
       		case DIRECT:
       		{
       			buffer = new DirectNIOBuffer(_bufferSize);
       			buffer.setServiceType(Buffers.ServiceType.BODY);
       			buffer.setBufferType(_bufferType);
       			return buffer;
       		}
       		case INDIRECT:
       		{
       			buffer = new IndirectNIOBuffer(_bufferSize);
       			buffer.setServiceType(Buffers.ServiceType.BODY);
       			buffer.setBufferType(_bufferType);
       			return buffer;
       		}
       }
       throw new IllegalStateException();
    }

    /* ------------------------------------------------------------ */
    /**
     * Create a new content Buffer
     * @param size
     * @return new Buffer
     */
    final protected Buffer newBuffer(int size)
    {
    	Buffer buffer;
    	switch(_otherType)
    	{
  			case BYTE_ARRAY:
  			{
  				buffer = new ByteArrayBuffer(size);
  				buffer.setServiceType(Buffers.ServiceType.BODY);
  				buffer.setBufferType(_otherType);
  				return buffer;
  			}
  			case DIRECT:
  			{
  				buffer = new DirectNIOBuffer(size);
  				buffer.setServiceType(Buffers.ServiceType.BODY);
  				buffer.setBufferType(_otherType);
  				return buffer;
  			}
  			case INDIRECT:
  			{
  				buffer = new IndirectNIOBuffer(size);
  				buffer.setServiceType(Buffers.ServiceType.BODY);
  				buffer.setBufferType(_otherType);
  				return buffer;
  			}
       }
       throw new IllegalStateException();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param buffer
     * @return True if the buffer is the correct type to be a Header buffer
     */
    public final boolean isHeader(Buffer buffer)
    {
        if (buffer.getServiceType() == Buffers.ServiceType.HEADER)
        {
            switch(buffer.getBufferType())
            {
                case BYTE_ARRAY:
                    return buffer instanceof ByteArrayBuffer && !(buffer instanceof  IndirectNIOBuffer);
                case DIRECT:
                    return buffer instanceof  DirectNIOBuffer;
                case INDIRECT:
                    return buffer instanceof  IndirectNIOBuffer;
            }
        }
        return false;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param buffer
     * @return True if the buffer is the correct type to be a Header buffer
     */
    public final boolean isBuffer(Buffer buffer)
    {
    	if (buffer.getServiceType() == Buffers.ServiceType.BODY)
        {
            switch(buffer.getBufferType())
            {
                case BYTE_ARRAY:
                    return buffer instanceof ByteArrayBuffer && !(buffer instanceof  IndirectNIOBuffer);
                case DIRECT:
                    return buffer instanceof  DirectNIOBuffer;
                case INDIRECT:
                    return buffer instanceof  IndirectNIOBuffer;
            }
        }
        return false;
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return String.format("%s [%d,%d]", getClass().getSimpleName(), _headerSize, _bufferSize);
    }
}
