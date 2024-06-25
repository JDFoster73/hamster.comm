package test.hamster.comm.stream.durable;

import org.junit.Test;
import org.slf4j.Logger;

import hamster.comm.buffer.durable.DurableBufferSession;
import hamster.comm.buffer.factory.StandardExpandableBufferFactoryCreator;
import test.hamster.emul.netchannel.TestChannel;
import test.hamster.emul.netchannel.TestCommPacketSequential;
import test.hamster.emul.netchannel.durable.TestChannelDurableAdapter;
import test.hamster.util.log.TestLogFormatter;
import test.hamster.util.log.TestLogUtil;

public class EmulatedDurableBufferSessionReconnectTest
{
  //@Test
  public void reconnectWithPendingAndACKNotProcessed() throws InterruptedException
  {
    
  }

  //@Test
  public void reconnectWithPendingAndACKProcessed() throws InterruptedException
  {
    
  }

  //@Test
  public void reconnectWithNoPendingAndACKNotProcessed() throws InterruptedException
  {
    
  }

  @Test
  public void reconnectWithNoPendingAndACKProcessed() throws InterruptedException
  {
    Logger logger = TestLogUtil.retrieveLogger("GENERAL", new TestLogFormatter("[GENERAL]"));
    
    // Create message checker objects.
    TestCommPacketSequential side1_to_side2_check = new TestCommPacketSequential("A>B TESTER", 80, 100, logger);

    //Durables for each endpoint.
    DurableBufferSession side1 = new DurableBufferSession(logger, "A", StandardExpandableBufferFactoryCreator.directBufferFactory(1000), (r) -> {});
    DurableBufferSession side2 = new DurableBufferSession(logger, "B", StandardExpandableBufferFactoryCreator.directBufferFactory(1000), side1_to_side2_check);
    
    //Create a test channel to send data from one buffer to another.  Allows for partial unsent data to be discarded for reconnect check purposes.
    TestChannel tc = new TestChannel(2000, 2000, null, logger);
    
    //Initialise both ends.
    side1.reinitialiseCommunicationState(new TestChannelDurableAdapter(tc.getA()));
    side2.reinitialiseCommunicationState(new TestChannelDurableAdapter(tc.getB()));

    //Advance tc to get INI messages to each end.
    tc.advance();
    
    //Process INIs.
    side1.handleCommunicationDataRead(tc.getA());
    side2.handleCommunicationDataRead(tc.getB());
    
    //Send some data from 1 > 2, transfer fully, handle it.
    side1.constructOutgoingMessage(side1_to_side2_check);
    side1.constructOutgoingMessage(side1_to_side2_check);
    side1.constructOutgoingMessage(side1_to_side2_check);
    side1.continueSending();
    side2.continueSending();
    
    tc.advance();

    side1.handleCommunicationDataRead(tc.getA());
    side2.handleCommunicationDataRead(tc.getB());
    
    side1.continueSending();
    side2.continueSending();
    tc.advance();

    //Process ACK message
    side1.handleCommunicationDataRead(tc.getA());
    side2.handleCommunicationDataRead(tc.getB());
    
    //All square.  Disconnect/reconnect and process INI.
  }
}
