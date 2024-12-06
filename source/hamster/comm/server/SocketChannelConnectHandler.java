package hamster.comm.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;

import hamster.comm.communication.ChannelCreateException;
import hamster.comm.itf.listener.ClientSocketChannelListener;
import hamster.comm.itf.listener.SocketChannelListener;

class SocketChannelConnectHandler 
{  
  private final ClientSocketChannelListener channelListener;

  private final SelectionKey registeredKey;

  private final Logger logger;
  
  private final SocketChannel sc;

//  private boolean channelClosed = false;
  
  SocketChannelConnectHandler(SelectionKey registeredKey, SocketChannel sc, ClientSocketChannelListener channelListener, Logger logger)
  {
    this.registeredKey = registeredKey;
    this.sc = sc;
    
    //this.bufferCreator = bufferCreator;
    
    this.channelListener = channelListener;
    this.logger = logger;

  }
  
  void handleChannelConnectEvent()
  {
    //Attempt to finish connecting.
    try
    {
      //Finish connecting.
      sc.finishConnect();
      
      //Configure for non-blocking.
      sc.configureBlocking(false);
      
      //Register with the selector.
      registeredKey.interestOps(SelectionKey.OP_READ);
      
      //Create a selection key attachment handler for the new channel.
      SocketChannelListener handleIncomingConnection = channelListener.handleConnectionSuccess(sc.getRemoteAddress());
      SocketChannelReadWriteHandler srwch = new SocketChannelReadWriteHandler(registeredKey, sc, handleIncomingConnection, logger);
      
      //This connection object is finished.  Replace the attachment in the selection key with the read/write handler.
      registeredKey.attach(srwch);
      
      //Finished connecting.  Initialise the controller.
      handleIncomingConnection.initController(srwch);
      
    } catch (IOException | ChannelCreateException e)
    {
      //Log this.
      
      //Cancel the key.
      registeredKey.cancel();
      
      //Connection failed.
      channelListener.handleConnectionFailure(e.getMessage());
    }
  }
  
}
