package test.hamster.comm.internalchannel;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

import org.junit.Test;

import hamster.comm.internalchannel.InternalChannelConnection;
import hamster.comm.internalchannel.InternalChannelServer;

public class InternalChannelTest
{
  @Test
  public void test() throws IOException
  {
    //Get the internal channel server.
    InternalChannelServer ics = InternalChannelServer.getServer();
    
    //Get TEST_CHANNELS channels, test they are connected to each other, then bin them.
    int TEST_CHANNELS = 100;
    
    ByteBuffer bb = ByteBuffer.allocate(8);
    
    SecureRandom srand = new SecureRandom();
    
    for(int i = 0; i < TEST_CHANNELS; i++)
    {
      //Get the connection.
      InternalChannelConnection internalConnection = ics.getInternalConnection();
      
      //Get the authenticator.
      long authenticator = srand.nextLong();
      
      //Write to buffer.
      bb.clear();
      bb.putLong(authenticator);
      bb.flip();
      while(bb.hasRemaining())
      {
        internalConnection.getAEnds().write(bb);
      }
      
      //Clear buffer.
      bb.clear();
      
      //Read back through B ends.
      while(bb.hasRemaining())
      {
        internalConnection.getBEnds().read(bb);
      }
      
      //Flip buffer to read.
      bb.flip();
      assertTrue(authenticator == bb.getLong());
      
      //Close.
      
      internalConnection.getAEnds().close();
      internalConnection.getBEnds().close();
    }
  }
}
