package test.hamster.comm.stream.durable;

import hamster.comm.buffer.durable.DurableSessionSender;
import hamster.comm.server.lst.ReadTransferHandler;
import test.hamster.emul.netchannel.TestChannel;
import test.hamster.emul.netchannel.durable.TestChannelDurableAdapter;

public abstract class NetEmulatingTestLoop
{
  private final TestChannel testChannel;

  private int loopCount = 0;
  
  public NetEmulatingTestLoop(TestChannel testChannel)
  {
    this.testChannel = testChannel;
  }

  public void runTestLoop()
  {
    //Initial 'connect.'
    handleAReinit(new TestChannelDurableAdapter(testChannel.getA()));
    handleBReinit(new TestChannelDurableAdapter(testChannel.getB()));
    
    // Number of runs.
    while(shouldKeepRunning())
    {
      System.out.println("--- LOOP " + loopCount + " ---");
      
      if(shouldRestartConnection())
      {
        //Disconnect and immediately reconnect.
        System.out.println("CLOSING TEST CHANNEL: " + testChannel.toString());

//        System.out.println("A>B STATE: " + side1.toString() + "   " + side1.outputReceiveBuffer());
//        
//        System.out.println("B>A STATE: " + side2.toString() + "   " + side2.outputReceiveBuffer());

        
        //"close" and "reopen" the channel again.
        testChannel.close();
        
        //Notify connection restarted.
        connectionRestarted();
        
        //Re-initialise the senders for the durable endpoints.
        handleAReinit(new TestChannelDurableAdapter(testChannel.getA()));
        handleBReinit(new TestChannelDurableAdapter(testChannel.getB()));
      }
      
      //Handle loop actions like sending data.
      handleStartLoop();
      
      //Gather writable message data from the endpoints.
      if(testChannel.getA().writeCallback()) continueASend();
      if(testChannel.getB().writeCallback()) continueBSend();
      
      //Advance the test channel.
      testChannel.advance();

      //Readable data to incoming handlers.
      if(testChannel.getA().has())
      {
        handleAIncoming(testChannel.getA());
      }
      if(testChannel.getB().has()) 
      {
        handleBIncoming(testChannel.getB());
      }
      
      try
      {
        Thread.sleep(1);
      }
      catch (InterruptedException e)
      {
        //Won't happen
      }
      
      //Increment loop counter.
      loopCount++;
    }
  }
  
  /**
   * <p>Test the implementation to see if we should continue running the loop.
   * 
   * @return implementation returns <code>true</code> if the loop should continue for another iteration.
   */
  protected abstract boolean shouldKeepRunning();
  
  /**
   * <p>Handle any implementation-specific code at the top of the loop.
   */
  protected abstract void handleStartLoop();
  
  /**
   * <p>Continue sending data on 'A' channel - space to send data into if available in outgoing session buffer.
   */
  protected abstract void continueASend();
  
  /**
   * <p>Continue sending data on 'B' channel - space to send data into if available in outgoing session buffer.
   */
  protected abstract void continueBSend();
  
  /**
   * <p>Handle incoming data on 'A' channel - analogous to the non-blocking OP_READ operation.
   */
  protected abstract void handleAIncoming(ReadTransferHandler chnl);
  
  /**
   * <p>Handle incoming data on 'B' channel - analogous to the non-blocking OP_READ operation.
   */
  protected abstract void handleBIncoming(ReadTransferHandler chnl);

  /**
   * <p>(Re-)initialise the 'A' channel - channel has (re-)'connected'.
   */
  protected abstract void handleAReinit(DurableSessionSender snd);

  /**
   * <p>(Re-)initialise the 'B' channel - channel has (re-)'connected'.
   */
  protected abstract void handleBReinit(DurableSessionSender snd);
  
  /**
   * <p>The emulated connection mechanism should restart.
   * 
   * @return
   */
  protected abstract boolean shouldRestartConnection();
  
  /**
   * <p>Notify the subclass that the connection has been restarted.
   */
  protected abstract void connectionRestarted();
  
  /**
   * <p>Return the number of bytes that are currently in transit from A to B.
   * @return
   */
  protected int aToBTransferData()
  {
    return testChannel.aToBTransferData();
  }

  /**
   * <p>Give subclasses convenience access to loop counter.  They can easily do this themselves but probably most of them will want to
   * so have it as a service.
   * 
   * @return
   */
  protected int loopCount()
  {
    return loopCount;
  }
}


