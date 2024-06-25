package test.hamster.emul.netchannel;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ResourceBundle;

import org.slf4j.Logger;

/**
 * <p>Instances of {@link TestChannel} endeavour to emulate a network connection between two endpoints.  These endpoints implement
 * {@link ReadableByteChannel} and {@link WritableByteChannel} and can be used as replacements to test {@link SocketChannel} connection
 * bi-directional data flow.
 * <p>The behaviour of these endpoints can be manipulated through their owning {@link TestChannel} instance.  They are designed to be used
 * in a loop which calls the {@link TestChannel#advance()} method to trigger data transfer from the 'A' side of the channel to the 'B' side
 * of the channel and visa-versa.
 *  
 * @author jdf19
 *
 */
public class TestChannel
{
  private final TestChannelEnd A;
  
  private final TestChannelEnd B;

  private final TestChannelBufferBridge aToB;
  
  private final TestChannelBufferBridge bToA;
  
  private final Logger logger;
  
  private boolean traceOut = false;
  
  public TestChannel(int bufferSize, int bridgeSize, TransferDecision tdec, Logger logger)
  {
    //Check for null decision.  If so then create decision which transfers as much as poss.
    if(tdec == null)
    {
      tdec = ((a, b, c) -> {return Math.min(b, c);});
    }
    
    //Create the buffer bridges.
    aToB = new TestChannelBufferBridge(ByteBuffer.allocate(bufferSize), ByteBuffer.allocate(bridgeSize), tdec, logger);
    bToA = new TestChannelBufferBridge(ByteBuffer.allocate(bufferSize), ByteBuffer.allocate(bridgeSize), tdec, logger);

    //Create channel ends.
    this.A = new TestChannelEnd(aToB, bToA, logger);
    this.B = new TestChannelEnd(bToA, aToB, logger);
    
    this.logger = logger;
  }
  
  public TestChannelEnd getA()
  {
    return A;
  }

  public TestChannelEnd getB()
  {
    return B;
  }
  
  /**
   * <p>Advance the simulated connection by one time period.
   */
  public void advance()
  {
    if(traceOut && logger.isTraceEnabled()) logger.trace(ResourceBundle.getBundle("test.hamster.emul.netchannel.strings").getString("tc.advance.1"), aToB, bToA);
    aToB.advance();
    bToA.advance();
    if(traceOut && logger.isTraceEnabled()) logger.trace(ResourceBundle.getBundle("test.hamster.emul.netchannel.strings").getString("tc.advance.2"), aToB, bToA);
  }

  public void close()
  {
    //Emulate a channel close.
    aToB.reset();
    bToA.reset();
  }

  @Override
  public String toString()
  {
    return "TestChannel [aToB=" + aToB + ", bToA=" + bToA + "]";
  }

  public boolean inTransit()
  {
    return (aToB.has() || bToA.has());
  }

  public int aToBTransferData()
  {
    // TODO Auto-generated method stub
    return aToB.bytesOfTransferData();
  }
  
  
}
