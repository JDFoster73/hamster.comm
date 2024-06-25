package test.hamster.comm.buffers.block;

import static org.junit.Assert.*;

import org.junit.Test;

public class FillDrainPipelineBufferTest
{

  @Test
  public void test()
  {
    int i1_1 = -1;
    int i1_2 = 209532098;

    int i2_1 = 325325324;
    int i2_2 = -1242214;

    int i3_1 = -1543;
    int i3_2 = -1;

    int i4_1 = 543624234;
    int i4_2 = 209532098;
    
    //Get the keys.
    long k1 = getKey(i1_1, i1_2);
    long k2 = getKey(i2_1, i2_2);
    long k3 = getKey(i3_1, i3_2);
    long k4 = getKey(i4_1, i4_2);
    
    int t1, t2;
    
    //Check first lot.
    t1 = getI1(k1);
    t2 = getI2(k1);
    
    assertTrue(t1 == i1_1);
    assertTrue(t2 == i1_2);

    //Check second lot.
    t1 = getI1(k2);
    t2 = getI2(k2);
    
    assertTrue(t1 == i2_1);
    assertTrue(t2 == i2_2);

    //Check third lot.
    t1 = getI1(k3);
    t2 = getI2(k3);
    
    assertTrue(t1 == i3_1);
    assertTrue(t2 == i3_2);

    //Check forth lot.
    t1 = getI1(k4);
    t2 = getI2(k4);
    
    assertTrue(t1 == i4_1);
    assertTrue(t2 == i4_2);
  }

  private long getKey(int i1, int i2)
  {
    //return ( ((long)i2) << 32) | (i1 & 0xffffffffL);
    return ( ((long)i2) << 32) | (i1 & 0xffffffffL);
  }

  private int getI1(long key)
  {
    return (int) (key & 0xffffffff);
  }

  private int getI2(long key)
  {
    return (int) ( (key >> 32) & 0xffffffff);
  }
}
