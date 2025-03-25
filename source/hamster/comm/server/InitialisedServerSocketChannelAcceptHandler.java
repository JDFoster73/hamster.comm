package hamster.comm.server;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;

import hamster.comm.KeyHelper;
import hamster.comm.communication.ChannelCreateException;
import hamster.comm.itf.controller.ServerSocketChannelController;
import hamster.comm.itf.listener.InitialisedServerSocketChannelListener;
import hamster.comm.itf.listener.SocketChannelListener;

class InitialisedServerSocketChannelAcceptHandler implements ServerSocketChannelController
{
  private final InitialisedServerSocketChannelListener channelListener;
  
  private final SelectionKey registeredKey;
  
  private final Logger logger;
  
  private final SocketAddress localAddress;
  
  InitialisedServerSocketChannelAcceptHandler(SocketAddress localAddress, SelectionKey registeredKey, InitialisedServerSocketChannelListener channelListener, Logger logger)
  {
    this.localAddress = localAddress;
    this.registeredKey = registeredKey;
    //this.bufferCreator = bufferCreator;
    
    //this.endListener = endListener;
    this.channelListener = channelListener;
    this.logger = logger;
  }
  
  void handleChannelAcceptEvent()
  {
    SocketChannel sc = null;
    
    try
    {
      //Accept the new channel.
      sc = getSocketChannel().accept();
    } catch (IOException e)
    {
      //The server socket is unusable.  Close.
      shut();
      
      //End.
      return;
    }
    
    SelectionKey acceptedChannelKey = null;
    
    try
    {
      //Configure for non-blocking.
      sc.configureBlocking(false);
      
      //Register with the selector.
      acceptedChannelKey = sc.register(registeredKey.selector(), SelectionKey.OP_READ);
      
      //Create a selection key attachment handler for the new channel.
      SocketChannelListener handleIncomingConnection = channelListener.handleIncomingConnection(sc.getRemoteAddress());
      SocketChannelReadWriteHandler srwch = new SocketChannelReadWriteHandler(acceptedChannelKey, sc, handleIncomingConnection, logger);
      
      //Attach the event handler to the channel selection key.
      acceptedChannelKey.attach(srwch);
      
      //Finished setting up the channel.  Call the listener init controller method.
      handleIncomingConnection.initController(srwch);
    } 
    catch (ChannelCreateException e)
    {
      //Log this.
      logger.error(BundleHelper.retrieveBundleFromClassPackage(NonBlockingCommunicationApplicationServer.class, "strings").getString("commbase.0010_0000.accepterr"), e.getMessage());
      
      //Cancel key.
      acceptedChannelKey.cancel();
    } 
    catch (IOException e)
    {
      //Cancel key.
      acceptedChannelKey.cancel();
      
      //Log this.
      logger.error(BundleHelper.retrieveBundleFromClassPackage(NonBlockingCommunicationApplicationServer.class, "strings").getString("commbase.0010_0001.registererr"), e.getMessage());

      //The client socket is unusable.  Close.
      try
      {
        sc.close();
      } catch (IOException e1)
      {
        // Annoying.  
      }
    }
    
  }
  
  private ServerSocketChannel getSocketChannel()
  {
    return ((ServerSocketChannel) registeredKey.channel());
  }
  
  @Override
  public SocketAddress getLocalAddress()
  {
    // TODO Auto-generated method stub
    return localAddress;
  }

  /**
   * Shut down the listening socket.
   */
  @Override
  public void shut()
  {
    //Close the channel.
    try
    {
      getSocketChannel().close();
    } catch (IOException e)
    {
      // TODO LOGGIT
    }
    
    //Cancel the key.
    registeredKey.cancel();
    
    //Notify listener of problem.
    channelListener.hasShut();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServerSocketChannel liberate()
  {
    //Cancel the selection key.
    registeredKey.cancel();
    
    //Return the channel.
    return getSocketChannel();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void pauseIncoming()
  {
    //Cancel OP_ACCEPT.
    KeyHelper.clearAcceptability(registeredKey);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resumeIncoming()
  {
    //Set OP_ACCEPT.
    KeyHelper.setAcceptability(registeredKey);
  }
}
