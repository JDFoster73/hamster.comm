package test.hamster.comm.channels.channelop;

public class TestVals
{
  private final long[] longVals;
  
  public TestVals(int numVals)
  {
    longVals = new long[numVals];
    
    for(int i = 0; i < numVals; i++)
    {
      longVals[i] = (long) (Math.random() * 0xf837428343L);
    }
  }

  public long get(int i)
  {
    // TODO Auto-generated method stub
    return longVals[i];
  }
}
