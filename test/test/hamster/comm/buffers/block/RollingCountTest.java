package test.hamster.comm.buffers.block;

import static org.junit.Assert.*;

import org.junit.Test;

import hamster.comm.RollingCounter;

public class RollingCountTest
{

  @Test
  public void testRollover()
  {
    RollingCounter rc = new RollingCounter(4, 6);

    assertTrue(rc.getCurrentCount() == 4);
    
    assertTrue(rc.advanceCount() == 5);
    assertTrue(rc.advanceCount() == 6);
    assertTrue(rc.advanceCount() == 4);
    assertTrue(rc.advanceCount() == 5);
    assertTrue(rc.advanceCount() == 6);
    assertTrue(rc.advanceCount() == 4);
  }

  @Test
  public void testNext()
  {
    RollingCounter rc = new RollingCounter(4, 6);
    
    assertTrue(rc.advanceCount() == 5);
    assertTrue(rc.advanceCount() == 6);

    //Test rollover without advancing.
    assertTrue(rc.peekNextCount(6) == 4);
    //Do rollover and test.
    assertTrue(rc.advanceCount() == 4);
    
  }
  
}
