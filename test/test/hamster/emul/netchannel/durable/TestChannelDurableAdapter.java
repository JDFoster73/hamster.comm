package test.hamster.emul.netchannel.durable;

import java.io.IOException;

import hamster.comm.buffer.block.itf.DrainableChannelBuffer;
import hamster.comm.buffer.durable.DurableSessionSender;
import test.hamster.emul.netchannel.TestChannelEnd;

public class TestChannelDurableAdapter implements DurableSessionSender
{
  private final TestChannelEnd senderEnd;
  
  public TestChannelDurableAdapter(TestChannelEnd senderEnd)
  {
    super();
    this.senderEnd = senderEnd;
  }

  @Override
  public int sendOutgoingCommunicationData(DrainableChannelBuffer writer)
  {
    // TODO Auto-generated method stub
    try
    {
      return writer.drainBufferToChannel(senderEnd);
    }
    catch(IOException e)
    {
      //Test framework doesn't throw this.
      throw new RuntimeException();
    }
  }

  @Override
  public void setOutgoingLoopEndNotification(boolean notificationOn)
  {
    // TODO Auto-generated method stub
    this.senderEnd.setWritableCallback(notificationOn);
  }

}
