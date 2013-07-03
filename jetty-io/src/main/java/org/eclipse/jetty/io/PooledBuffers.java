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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class PooledBuffers extends AbstractBuffers
{
	private static final Logger logger = Log.getLogger(PooledBuffers.class);
    private final Queue<Buffer> _headers;
    private final Queue<Buffer> _buffers;
    private final Queue<Buffer> _others;
    private final AtomicInteger _size = new AtomicInteger();
    private final int _maxSize;
    private final boolean _otherHeaders;
    private final boolean _otherBuffers;
    
    private final AtomicLong _headRatio = new AtomicLong(0);
    private final AtomicLong _headCount = new AtomicLong(0);
    private final AtomicLong _bufferRatio = new AtomicLong(0);
    private final AtomicLong _bufferCount = new AtomicLong(0);
    private final AtomicLong _allocDirectBufferSize = new AtomicLong(0);
    private long maxDirectBufferSize = 0;//最大Directbuffer设置，如果是0不起效,单位k
    private boolean needAdjustBufferSize = false;
    private int maxBufferIdleTime = 30 *60;
    private int checkInterval = 10;

    /* ------------------------------------------------------------ */
    public PooledBuffers(Buffers.Type headerType, int headerSize, Buffers.Type bufferType, int bufferSize, Buffers.Type otherType,int maxSize)
    {
        super(headerType,headerSize,bufferType,bufferSize,otherType);
        _headers=new ConcurrentLinkedQueue<Buffer>();
        _buffers=new ConcurrentLinkedQueue<Buffer>();
        _others=new ConcurrentLinkedQueue<Buffer>();
        _otherHeaders=headerType==otherType;
        _otherBuffers=bufferType==otherType;
        _maxSize=maxSize;

        getConfigFromSystemCommandLine();

        Thread t = new Thread(new BufferInspactor(checkInterval));
        t.setDaemon(true);
        t.start();  
    }
    
    public int[] getInnerCounters()
    {
    	int[] result = new int[4];
    	
    	result[0] = _headers.size();
    	result[1] = _buffers.size();
    	result[2] = _others.size();
    	result[3] = _size.get(); 	
    	
    	return result;
    }
    
    public void getConfigFromSystemCommandLine()
    {
    	if (System.getProperty("jetty.maxDirectBufferSize") != null)
		{
        	maxDirectBufferSize = Long.valueOf(System.getProperty("jetty.maxDirectBufferSize"));
			logger.warn("jetty._maxDirectBufferSize :" + maxDirectBufferSize);
		}
    	if (System.getProperty("jetty.needAdjustBufferSize") != null)
		{
        	needAdjustBufferSize = Boolean.valueOf(System.getProperty("jetty.needAdjustBufferSize"));
			logger.warn("jetty PoolBuffer needAdjustBufferSize ");
		}  
    	if (System.getProperty("jetty.PooledBufferInspactorInterval") != null)
		{
        	checkInterval = Integer.valueOf(System.getProperty("jetty.PooledBufferInspactorInterval"));
			logger.warn("jetty.PooledBufferInspactorInterval :" + checkInterval);
		}   
        if (System.getProperty("jetty.maxBufferIdleTime") != null)
		{
        	maxBufferIdleTime = Integer.valueOf(System.getProperty("jetty.maxBufferIdleTime"));
			logger.warn("jetty.maxBufferIdleTime :" + maxBufferIdleTime);
		} 
    }
    
    

    public long getMaxDirectBufferSize() {
		return maxDirectBufferSize;
	}

	public void setMaxDirectBufferSize(long maxDirectBufferSize) {
		this.maxDirectBufferSize = maxDirectBufferSize;
	}

	public boolean isNeedAdjustBufferSize() {
		return needAdjustBufferSize;
	}

	public void setNeedAdjustBufferSize(boolean needAdjustBufferSize) {
		this.needAdjustBufferSize = needAdjustBufferSize;
	}

	public int getMaxBufferIdleTime() {
		return maxBufferIdleTime;
	}

	public void setMaxBufferIdleTime(int maxBufferIdleTime) {
		this.maxBufferIdleTime = maxBufferIdleTime;
	}

	public int getCheckInterval() {
		return checkInterval;
	}

	public void setCheckInterval(int checkInterval) {
		this.checkInterval = checkInterval;
	}

	/* ------------------------------------------------------------ */
    public Buffer getHeader()
    {
        Buffer buffer = _headers.poll();
        if (buffer==null)
        {
        	if (maxDirectBufferSize > 0)
        		if (_headerType == Buffers.Type.DIRECT)
        		{
        			if (_allocDirectBufferSize.addAndGet(_headerSize) > maxDirectBufferSize * 1024)
        			{
        				_headerType = Buffers.Type.BYTE_ARRAY;
        				_allocDirectBufferSize.addAndGet(-_headerSize);
        				
        				logger.warn(new StringBuilder("jetty direct buffer alloc out of limit(k) :").append(maxDirectBufferSize).toString());
        			}
        				
        		}
        	
        	
        	buffer=newHeader();
        }  
        else
            _size.decrementAndGet();
        return buffer;
    }

    /* ------------------------------------------------------------ */
    public Buffer getBuffer()
    {
        Buffer buffer = _buffers.poll();
        
        if (buffer==null)
        {
        	if (maxDirectBufferSize > 0)
        		if (_bufferType == Buffers.Type.DIRECT)
        		{
        			if (_allocDirectBufferSize.addAndGet(_bufferSize) > maxDirectBufferSize * 1024)
        			{
        				_bufferType = Buffers.Type.BYTE_ARRAY;
        				_allocDirectBufferSize.addAndGet(-_bufferSize);
        				
        				logger.warn(new StringBuilder("jetty direct buffer alloc out of limit(k) :").append(maxDirectBufferSize).toString());
        			}
        				
        		}
        	
            buffer=newBuffer();
        }
        else
            _size.decrementAndGet();
        
        return buffer;
    }

    /* ------------------------------------------------------------ */
    public Buffer getBuffer(int size)
    {
        if (_otherHeaders && size==getHeaderSize())
            return getHeader();
        if (_otherBuffers && size==getBufferSize())
            return getBuffer();

        // Look for an other buffer
        Buffer buffer = _others.poll();

        // consume all other buffers until one of the right size is found
        while (buffer!=null && buffer.capacity()!=size)
        {
            _size.decrementAndGet();
            buffer = _others.poll();
        }

        if (buffer==null)
            buffer=newBuffer(size);
        else
            _size.decrementAndGet();
        return buffer;
    }

    /* ------------------------------------------------------------ */
    public void returnBuffer(Buffer buffer)
    {
    	buffer.setLastUseTimeStamp(System.currentTimeMillis());
    	
    	//统计使用率
    	if (isHeader(buffer))
    	{
    		try
        	{
        		_headRatio.addAndGet((buffer.putIndex()*100)/buffer.capacity());
        		_headCount.incrementAndGet();
        	}
        	catch(Exception ex)
        	{
        		_headRatio.set(0);
        		_headCount.set(0);
        	}
    	}
    	else if (isBuffer(buffer))
    	{
    		try
        	{
    			_bufferRatio.addAndGet((buffer.putIndex()*100)/buffer.capacity());
        		_bufferCount.incrementAndGet();
        	}
        	catch(Exception ex)
        	{
        		_bufferRatio.set(0);
        		_bufferCount.set(0);
        	}
    	}
    	
    	
        buffer.clear();
        if (buffer.isVolatile() || buffer.isImmutable())
            return;

        if (_size.incrementAndGet() > _maxSize)
            _size.decrementAndGet();
        else
        {
            if (isHeader(buffer))
                _headers.add(buffer);
            else if (isBuffer(buffer))
                _buffers.add(buffer);
            else
                _others.add(buffer);
        }
    }

    public String toString()
    {
        return String.format("%s [%d/%d@%d,%d/%d@%d,%d/%d@-]",
                getClass().getSimpleName(),
                _headers.size(),_maxSize,_headerSize,
                _buffers.size(),_maxSize,_bufferSize,
                _others.size(),_maxSize);
    }
    
    public void checkBufferChain()
    {
    	long current = System.currentTimeMillis();
    	
    	//first in ,first out, so just use queue order,but if check vaild ,it will add to tail,final it will be use or remove
    	if (_headerType == Buffers.Type.BYTE_ARRAY 
    			|| _headerType == Buffers.Type.INDIRECT)
    	{
    		Buffer buf = null;
    		Buffer firstBuffer = null;
    		
    		while((buf = _headers.peek()) != null && firstBuffer != buf)
    		{
    			if (buf.getLastUseTimeStamp() <= 0)
    			{
    				break;
    			}
    			else
    			{
    				if (current - buf.getLastUseTimeStamp() > maxBufferIdleTime * 1000)
    				{
    					Buffer temp = _headers.poll();
    					
    					if (temp == buf)
    					{
    						if (buf.getBufferType() != Buffers.Type.DIRECT)
    							_size.decrementAndGet();
    						else
    						{
    							_headers.add(temp);
    							
    							if (firstBuffer == null)
    								firstBuffer = temp;
    						}
    					}
    					else
    						if (temp != null)
    						{
    							_headers.add(temp);
    							
    							if (firstBuffer == null)
    								firstBuffer = temp;
    						}
    						else
    							break;
    				}
    				else
    				{
    					break;
    				}
    			}
    		}
    	}
    	
    	if (_bufferType == Buffers.Type.BYTE_ARRAY 
    			|| _bufferType == Buffers.Type.INDIRECT)
    	{
    		Buffer buf = null;
    		Buffer firstBuffer = null;
    		
    		while((buf = _buffers.peek()) != null && firstBuffer != buf)
    		{
    			if (buf.getLastUseTimeStamp() <= 0)
    			{
    				break;
    			}
    			else
    			{
    				if (current - buf.getLastUseTimeStamp() > maxBufferIdleTime * 1000)
    				{
    					Buffer temp = _buffers.poll();
    					
    					if (temp == buf)
    					{
    						if (buf.getBufferType() != Buffers.Type.DIRECT)
    							_size.decrementAndGet();
    						else
    						{
    							_buffers.add(temp);
    							
    							if (firstBuffer == null)
    								firstBuffer = temp;
    						}
    					}
    					else
    						if (temp != null)
    						{
    							_buffers.add(temp);
    							
    							if (firstBuffer == null)
    								firstBuffer = temp;
    						}
    						else
    							break;
    				}
    				else
    				{
    					break;
    				}
    			}
    		}
    	}
    }
    
    class BufferInspactor implements java.lang.Runnable
    {
    	private int interval = 10;

    	public BufferInspactor(int interval)
    	{
    		if (interval > 10)
    			this.interval = interval;
    	}
    	
		@Override
		public void run() {
			
			while(true)
			{
				
				try {
					if (needAdjustBufferSize)
					{
						checkBufferChain();
					}
					
					Thread.sleep(interval * 1000);
					
					logger.warn(new StringBuilder().append("PooledBuffers info: ")
							.append(" head chain count: ").append(_headers.size())
							.append(" body chain count: ").append(_buffers.size())
							.append(" head usage: ").append(_headRatio.get()/_headCount.get())
							.append(" ,body usage: ").append(_bufferRatio.get()/_bufferCount.get())
							.append(" ,direct buffer size: ").append(_allocDirectBufferSize.get()).toString());
				} catch (InterruptedException e) {
					logger.warn(e.getCause());
				}
			}
			
		}
    	
    }
}
