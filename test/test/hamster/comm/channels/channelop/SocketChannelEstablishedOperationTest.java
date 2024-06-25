package test.hamster.comm.channels.channelop;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import hamster.comm.itf.ctrl.SocketChannelController;
import hamster.comm.itf.lst.SocketChannelListener;
import hamster.comm.server.NonBlockingCommunicationServer;
import hamster.comm.server.exc.ChannelRegistrationException;
import hamster.comm.server.lst.ReadTransferHandler;

public class SocketChannelEstablishedOperationTest
{
  //private static TestVals testVals = new TestVals(4);
  
  private SocketChannel testClient;
  
  /**
   * <p>Simple socket test.  Create a {@link NonBlockingCommunicationServer} and register an established socket.
   * 
   * @throws IOException if thrown by the NonBlockingCommunicationServer.
   * @throws ChannelRegistrationException  if we couldn't register the channel.
   * @throws InterruptedException Joining comm thread threw this.
   */
  @Test
  public void simpleReceiveTest() throws IOException, ChannelRegistrationException, InterruptedException
  {
    //SET UP THE SERVER CHANNEL.
    final ServerSocketChannel ssc = ServerSocketChannel.open();
    ssc.bind(null);
    int port = ((InetSocketAddress) ssc.getLocalAddress()).getPort();
    
    Thread t = new Thread(() -> {
      //Listen.
      try
      {
        testClient = ssc.accept();
      }
      catch (IOException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
        fail();
      }
    });
    
    //Connect a client socket to the server channel.
    SocketChannel sc = SocketChannel.open(new InetSocketAddress("localhost", port));
    
    //Allow server to catch up.
    Thread.sleep(100);
    
    //Add the test client end to an NCBS.
    Selector s = Selector.open();
    NonBlockingCommunicationServer nbcs1 = new NonBlockingCommunicationServer(s, LoggerFactory.getLogger("STANDARD"));
    SocketChannelTestListener sctl = new SocketChannelTestListener(nbcs1);
    nbcs1.registerOpenSocket(sc, sctl);
    
    //Start nbcs1.
    Thread nbcs1Thread = new Thread(nbcs1);
    nbcs1Thread.start();
    
    
  }

  class SocketChannelTestListener extends AbstractChannelTestBedListener<SocketChannel> implements SocketChannelListener
  {

    public SocketChannelTestListener(NonBlockingCommunicationServer nbcs)
    {
      super(nbcs);
    }

    @Override
    public void isClosing()
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void hasShut()
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void handleDataRead(ReadTransferHandler transferHandler)
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void handleChannelWriteContinue()
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void wakeup(int parameter, long wakeupTime)
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void initController(SocketChannelController controller)
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void handleReadStop()
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void handleServerLoopEnd()
    {
      // TODO Auto-generated method stub
      
    }
    
  }
}
