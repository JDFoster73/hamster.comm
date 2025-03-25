package test.hamster.comm.internalchannel;

import hamster.comm.internalchannel.BlockingReadSelector;
import hamster.comm.internalchannel.InternalChannelConnection;
import hamster.comm.internalchannel.InternalChannelFactory;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ChannelTests
{
  @Test
  public void selectorTest() throws IOException
  {
    InternalChannelConnection connection = InternalChannelFactory.createConnection();

    BlockingReadSelector brs = new BlockingReadSelector();

    Thread t = new Thread(() -> {
      ByteBuffer bb = ByteBuffer.allocate(10);
      try
      {
        Thread.sleep(1500);
        bb.putInt(2255);
        bb.flip();
        connection.getBEnds().write(bb);

        Thread.sleep(1500); bb.clear();
        bb.putInt(33455);
        bb.flip();
        connection.getBEnds().write(bb);
      }
      catch (IOException e)
      {
        throw new RuntimeException(e);
      }
      catch (InterruptedException e)
      {
        throw new RuntimeException(e);
      }
    }, "sender");
    t.start();

    while(brs.checkReadable(connection.getAEnds(),3000))
    {
      ByteBuffer bb = ByteBuffer.allocate(10);
      connection.getAEnds().read(bb);
      bb.flip();
      int i = bb.getInt();
      System.out.println("Received " + i);
    }

//    brs.checkReadable(1000);
//    brs.checkReadable(1000);

    {
      System.out.println("Nothing there!");
    }

  }
}
