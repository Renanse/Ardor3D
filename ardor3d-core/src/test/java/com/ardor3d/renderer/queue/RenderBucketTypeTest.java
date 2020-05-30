
package com.ardor3d.renderer.queue;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

public class RenderBucketTypeTest {

  @Test
  public void getPrebucket() throws Exception {
    final RenderBucketType preBucket = RenderBucketType.getRenderBucketType("PreBucket");
    assertNotNull(preBucket);

    final RenderBucketType preBucket2 = RenderBucketType.getRenderBucketType("PreBucket");
    assertSame(preBucket, preBucket2);
    assertSame(RenderBucketType.PreBucket, preBucket);
  }

  @Test
  public void getUserDefined() throws Exception {
    final RenderBucketType myBucket = RenderBucketType.getRenderBucketType("MyBucket");
    assertNotNull(myBucket);

    final RenderBucketType myBucket2 = RenderBucketType.getRenderBucketType("MyBucket");
    assertSame(myBucket, myBucket2);
  }

  @Test
  public void getWithNull() throws Exception {
    try {
      RenderBucketType.getRenderBucketType(null);
      fail();
    } catch (final IllegalArgumentException e) {}
  }
}
