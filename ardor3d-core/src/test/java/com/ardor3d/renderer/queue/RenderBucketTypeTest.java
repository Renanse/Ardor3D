package com.ardor3d.renderer.queue;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class RenderBucketTypeTest {

	@Test
	public void getPrebucket() throws Exception {
		RenderBucketType preBucket = RenderBucketType.getRenderBucketType("PreBucket");
		assertNotNull(preBucket);

		RenderBucketType preBucket2 = RenderBucketType.getRenderBucketType("PreBucket");
		assertSame(preBucket, preBucket2);
		assertSame(RenderBucketType.PreBucket, preBucket);
	}

	@Test
	public void getUserDefined() throws Exception {
		RenderBucketType myBucket = RenderBucketType.getRenderBucketType("MyBucket");
		assertNotNull(myBucket);

		RenderBucketType myBucket2 = RenderBucketType.getRenderBucketType("MyBucket");
		assertSame(myBucket, myBucket2);
	}

	@Test
	public void getWithNull() throws Exception {
		try {
			RenderBucketType nullBucket = RenderBucketType.getRenderBucketType(null);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}
}
