package test.hamster.comm.stream.durable;

import org.junit.Test;
import org.slf4j.Logger;

import hamster.comm.buffer.durable.DurableBufferSession;
import hamster.comm.buffer.durable.DurableSessionSender;
import hamster.comm.buffer.factory.StandardExpandableBufferFactoryCreator;
import hamster.comm.server.lst.ReadTransferHandler;
import test.hamster.emul.netchannel.TestChannel;
import test.hamster.emul.netchannel.TestCommPacketSequential;
import test.hamster.util.log.TestLogFormatter;
import test.hamster.util.log.TestLogUtil;

public class EmulatedDurableBufferSessionTest_2
{
  @Test
  public void bidirectionalDataTestOneConnection() throws InterruptedException
  {
    Logger logger = TestLogUtil.retrieveLogger("GENERAL", new TestLogFormatter("[GENERAL]"));
    
    logger.trace("Starting bidirectional emulated connection test.");
    
    // Create message checker objects.
    TestCommPacketSequential side1_to_side2_check = new TestCommPacketSequential("A>B TESTER", 80, 100, logger);
    TestCommPacketSequential side2_to_side1_check = new TestCommPacketSequential("B>A TESTER", 80, 100, logger);
//    TestCommPacketRandom side1_to_side2_check = new TestCommPacketRandom("A>B TESTER", 80, 100, logger);
//    TestCommPacketRandom side2_to_side1_check = new TestCommPacketRandom("B>A TESTER", 80, 100, logger);

    //Durables for each endpoint.
    DurableBufferSession side1 = new DurableBufferSession(logger, "A", StandardExpandableBufferFactoryCreator.directBufferFactory(1000), side2_to_side1_check);
    DurableBufferSession side2 = new DurableBufferSession(logger, "B", StandardExpandableBufferFactoryCreator.directBufferFactory(1000), side1_to_side2_check);
    
    DurableNetEmulatingTestLoopAdapter lp = new DurableNetEmulatingTestLoopAdapter(new TestChannel(150, 100, null, logger), logger, side1, side2)
    {
      @Override
      protected boolean shouldKeepRunning()
      {
        if(loopCount() < 1000) return true;
        
        return false;
      }
      
      @Override
      protected void handleStartLoop()
      {
        //Construct 3 messages in one go to start with.
        if(loopCount() == 0)
        {
          side1.constructOutgoingMessage(side1_to_side2_check);
          side1.constructOutgoingMessage(side1_to_side2_check);
          side1.constructOutgoingMessage(side1_to_side2_check);
        }
        
        if( (loopCount() > 2) && (loopCount() < 950) )
        {
          //Generate side 1 data.
          if(Math.random() <= 0.2)
          {
            side1.constructOutgoingMessage(side1_to_side2_check);
          }
          //Generate side 2 data.
          if(Math.random() <= 0.2) 
          {
            side2.constructOutgoingMessage(side2_to_side1_check);
          }
        }
      }
      
      @Override
      protected boolean shouldRestartConnection()
      {
        //10% chance of a disconnection.
        if( (loopCount() >= 3) && (Math.random() <= 0.1) && (aToBTransferData() > 10) )
        {
          return true;
        }
        
        return false;
      }

      @Override
      protected void connectionRestarted()
      {
        //No action
        int i = 1;
      }
    };
    
    //Run the loop.
    lp.runTestLoop();
  }
  
  //@Test
  public void partialSendDisconnectRecoveryTest() throws InterruptedException
  {
    Logger logger = TestLogUtil.retrieveLogger("GENERAL", new TestLogFormatter("[GENERAL]"));


    TestCommPacketSequential side1_to_side2_check = new TestCommPacketSequential("A>B TESTER", 80, 100, logger);
    TestCommPacketSequential side2_to_side1_check = new TestCommPacketSequential("B>A TESTER", 80, 100, logger);
    
    DurableBufferSession side1 = new DurableBufferSession(logger, "A", StandardExpandableBufferFactoryCreator.directBufferFactory(1000), side2_to_side1_check);
    DurableBufferSession side2 = new DurableBufferSession(logger, "B", StandardExpandableBufferFactoryCreator.directBufferFactory(1000), side1_to_side2_check);
    
    //Create an emulated test loop.
    DurableNetEmulatingTestLoopAdapter dnetla = new DurableNetEmulatingTestLoopAdapter(new TestChannel(150, 100, null, logger), logger, side1, side2)
    {
      
      @Override
      protected boolean shouldRestartConnection()
      {
        //Restart after the first message has been processed but the transit buffer is still full.
        if(loopCount() == 7)
        {
          return true;
        }
        return false;
      }
      
      @Override
      protected boolean shouldKeepRunning()
      {
        //Run to 10 loops to allow all message data to be fully sent and received.
        return loopCount() < 10;
      }
      
      @Override
      protected void handleStartLoop()
      {
        //Construct 3 messages in one go to start with.
        if(loopCount() == 0)
        {
          side1.constructOutgoingMessage(side1_to_side2_check);
          side1.constructOutgoingMessage(side1_to_side2_check);
          side1.constructOutgoingMessage(side1_to_side2_check);
          side1.constructOutgoingMessage(side1_to_side2_check);
          side1.constructOutgoingMessage(side1_to_side2_check);
          side1.constructOutgoingMessage(side1_to_side2_check);
        }
      }
      
      @Override
      protected void connectionRestarted()
      {
      }
    };
    
    //Run the test loop.
    dnetla.runTestLoop();
  }
  
  abstract class DurableNetEmulatingTestLoopAdapter extends NetEmulatingTestLoop
  {    
    DurableBufferSession side1;
    DurableBufferSession side2;

    protected int connCounter = 0;
    
    public DurableNetEmulatingTestLoopAdapter(TestChannel testChannel, Logger logger, DurableBufferSession side1, DurableBufferSession side2)
    {
      super(testChannel);
      this.side1 = side1;//new DurableBufferSession(logger, "A", StandardExpandableBufferFactoryCreator.directBufferFactory(1000), side2_to_side1_check);
      this.side2 = side2;// = new DurableBufferSession(logger, "B", StandardExpandableBufferFactoryCreator.directBufferFactory(1000), side1_to_side2_check);
    }
    
    @Override
    protected void handleBReinit(DurableSessionSender snd)
    {
      side2.reinitialiseCommunicationState(snd);
    }
    
    @Override
    protected void handleBIncoming(ReadTransferHandler chnl)
    {
      side2.handleCommunicationDataRead(chnl);
    }
    
    @Override
    protected void handleAReinit(DurableSessionSender snd)
    {
      side1.reinitialiseCommunicationState(snd);
      connCounter++;
    }
    
    @Override
    protected void handleAIncoming(ReadTransferHandler chnl)
    {
      side1.handleCommunicationDataRead(chnl);
    }
    
    @Override
    protected void continueBSend()
    {
      side2.continueSending();
    }
    
    @Override
    protected void continueASend()
    {
      side1.continueSending();
    }
  }
}
