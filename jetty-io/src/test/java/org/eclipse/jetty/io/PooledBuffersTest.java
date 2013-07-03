package org.eclipse.jetty.io;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PooledBuffersTest {

	PooledBuffers pooledBuffers;
	
	@Before
	public void setUp() throws Exception {
		pooledBuffers = new PooledBuffers(Buffers.Type.BYTE_ARRAY,1*1024,Buffers.Type.DIRECT,1*1024,Buffers.Type.INDIRECT,50);
		pooledBuffers.setNeedAdjustBufferSize(true);
		pooledBuffers.setMaxDirectBufferSize(40);
		pooledBuffers.setCheckInterval(20);
		pooledBuffers.setMaxBufferIdleTime(40);
	}

	@After
	public void tearDown() throws Exception {
		
		
	}

	@Test
	public void test() throws InterruptedException {
		Buffer[] header = new Buffer[40];
		Buffer[] body = new Buffer[45];
		
		for (int i =0 ; i < 10; i++)
		{
			header[i] = pooledBuffers.getHeader();
			body[i] = pooledBuffers.getBuffer();
		}
		
		Assert.assertArrayEquals(new int[]{0,0,0,0}, pooledBuffers.getInnerCounters());
		
		for (int i =0 ; i < 10; i++)
		{
			pooledBuffers.returnBuffer(header[i]);
			pooledBuffers.returnBuffer(body[i]);
		}
		
		Assert.assertArrayEquals(new int[]{10,10,0,20}, pooledBuffers.getInnerCounters());
		
		for (int i =0 ; i < 10; i++)
		{
			header[i] = pooledBuffers.getHeader();
			body[i] = pooledBuffers.getBuffer();
		}
		
		Assert.assertArrayEquals(new int[]{0,0,0,0}, pooledBuffers.getInnerCounters());
		
		for (int i =0 ; i < 10; i++)
		{
			pooledBuffers.returnBuffer(header[i]);
			pooledBuffers.returnBuffer(body[i]);
		}
		
		Assert.assertArrayEquals(new int[]{10,10,0,20}, pooledBuffers.getInnerCounters());
		
		for (int i =0 ; i < 30; i++)
		{
			header[i] = pooledBuffers.getHeader();
			body[i] = pooledBuffers.getBuffer();
		}
		
		Assert.assertArrayEquals(new int[]{0,0,0,0}, pooledBuffers.getInnerCounters());
		
		for (int i =0 ; i < 30; i++)
		{
			pooledBuffers.returnBuffer(header[i]);
			pooledBuffers.returnBuffer(body[i]);
		}
		
		Assert.assertArrayEquals(new int[]{25,25,0,50}, pooledBuffers.getInnerCounters());
		
		Thread.sleep(30 * 1000);
		
		Assert.assertArrayEquals(new int[]{25,25,0,50}, pooledBuffers.getInnerCounters());
		
		Thread.sleep(60 * 1000);
		
		Assert.assertArrayEquals(new int[]{0,25,0,25}, pooledBuffers.getInnerCounters());
		
		for (int i =0 ; i < 43; i++)
		{
			body[i] = pooledBuffers.getBuffer();
		}
		Assert.assertArrayEquals(new int[]{0,0,0,0}, pooledBuffers.getInnerCounters());
		
		int directcount = 0;
		int arraycount = 0;
		
		for (int i = 0; i < 43; i++)
		{
			if(body[i].getBufferType() == Buffers.Type.DIRECT)
				directcount +=1;
			else
				if (body[i].getBufferType() == Buffers.Type.BYTE_ARRAY)
					arraycount +=1;
		}
		
		//流失了5个，所以最多只有35个可以申请
		Assert.assertEquals(directcount, 35);
		Assert.assertEquals(arraycount, 8);
		
		for (int i =0 ; i < 43; i++)
		{
			pooledBuffers.returnBuffer(body[i]);
		}
		Assert.assertArrayEquals(new int[]{0,43,0,43}, pooledBuffers.getInnerCounters());
		
		Thread.sleep(60 * 1000);
		
		Assert.assertArrayEquals(new int[]{0,35,0,35}, pooledBuffers.getInnerCounters());
	}

}
