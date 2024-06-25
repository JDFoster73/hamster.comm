package test.hamster.comm.stream.durable;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.junit.Test;
import org.slf4j.Logger;

import hamster.comm.buffer.block.itf.DrainableChannelBuffer;
import hamster.comm.buffer.durable.DurableBufferSession;
import hamster.comm.buffer.durable.DurableSessionSender;
import hamster.comm.buffer.factory.StandardExpandableBufferFactoryCreator;
import test.hamster.emul.netchannel.TestCommPacketRandom;
import test.hamster.util.log.TestLogUtil;

public class DurableSessionBaseTests
{

  /**
   * <p>Check a durable buffer session responds correctly to an ACK telegram.
   */
  @Test
  public void manualReclaimTest()
  {
    Logger retrieveLogger = TestLogUtil.retrieveLogger("TEST");
    DurableBufferSession dbs = new DurableBufferSession(retrieveLogger, "A",  StandardExpandableBufferFactoryCreator.directBufferFactory(1000), (r) -> {});
    
    //Create a test packet generator/checker.
    TestCommPacketRandom tcpr = new TestCommPacketRandom("A", 80, 100, retrieveLogger);
    
    //Generate 4 messages.
    int wr = dbs.constructOutgoingMessage(tcpr);
    wr += dbs.constructOutgoingMessage(tcpr);
    wr += dbs.constructOutgoingMessage(tcpr);
    wr += dbs.constructOutgoingMessage(tcpr);
    
    //'Send' nearly 4 of them.
    Sender sd = new Sender();
    sd.setDrainMax(wr - 50);
    
    dbs.reinitialiseCommunicationState(new Sender());
  }

  class Sender implements DurableSessionSender, WritableByteChannel
  {

    private int drainMax;

    public void setDrainMax(int i)
    {
      this.drainMax = i;
    }

    @Override
    public int sendOutgoingCommunicationData(DrainableChannelBuffer writer)
    {
      try
      {
        return writer.drainBufferToChannel(this);
      }
      catch (IOException e)
      {
        return -1;
      }
    }

    @Override
    public void setOutgoingLoopEndNotification(boolean notificationOn)
    {
    }

    @Override
    public boolean isOpen()
    {
      return true;
    }

    @Override
    public void close() throws IOException
    {
    }

    @Override
    public int write(ByteBuffer src) throws IOException
    {
      //Skip the number of bytes.
      src.position(src.position() + drainMax);
      
      // TODO Auto-generated method stub
      return drainMax;
    }
    
  }
}
