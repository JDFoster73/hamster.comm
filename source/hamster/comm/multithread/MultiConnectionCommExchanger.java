package hamster.comm.multithread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import hamster.comm.buffer.block.GeneralPipelineBuffer;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.buffer.factory.StandardExpandableBufferFactoryCreator;
import hamster.comm.communication.ChannelCreateException;
import hamster.comm.itf.controller.ServerSocketChannelController;
import hamster.comm.itf.controller.SocketChannelController;
import hamster.comm.itf.listener.ClientSocketChannelListener;
import hamster.comm.itf.listener.InitialisedServerSocketChannelListener;
import hamster.comm.itf.listener.SocketChannelListener;
import hamster.comm.server.CommLoopInteractor;
import hamster.comm.server.NonBlockingCommunicationApplication;
import hamster.comm.server.NonBlockingCommunicationApplicationServer;
import hamster.comm.server.exception.ChannelRegistrationException;
import hamster.comm.server.listener.CommunicationApplicationController;

public class MultiConnectionCommExchanger
{
  public final Controller controller = new ControllerImpl();
  
  /**
   * <p>
   * Non-blocking communication server for serving comms to the server.
   */
  private final NonBlockingCommunicationApplicationServer nbcs;

  /**
   * Owner callback listener implementation.
   */
  private final CommExchangeEventListener listener;

  /**
   * Thread lock synchronisation object.
   */
  private final Object threadLock = new Object();

  /**
   * The application controller.
   */
  private CommunicationApplicationController appController;

  /**
   * Connection requests will be added to this list and taken off by the NBCAS and
   * processed.
   */
  private final List<ConnectionRequest> connectionList = new ArrayList<>();

  /**
   * <p>
   * The connection map. Lookup connection manager instances for a given
   * connection id.
   */
  private final Map<ConnectionID, ConnectionManager> connectionMap = new HashMap<>();

  /**
   * <p>
   * Construct the comm exchanger. This will create an underlying
   * {@link NonBlockingCommunicationApplicationServer} and run it in a separate
   * thread. Notifications to the owning thread will be made from the underlying
   * NBCAS via the listener parameter.
   * 
   * @param listener communication event notifications raised in the underlying
   *                 NBCAS will be caught by this owner-supplied callback
   *                 interface.
   * @param logger   event logger.
   * @throws IOException If the NBCAS could not be created and started.
   */
  public MultiConnectionCommExchanger(CommExchangeEventListener listener, Logger logger) throws IOException
  {
    // Save the listener reference.
    this.listener = listener;

    //Inject the controller.
    listener.init(controller);
    
    // The communication application registers an open socket and sets the loop
    // interactor.
    NonBlockingCommunicationApplication app = new NonBlockingCommunicationApplication()
    {

      /**
       * {@inheritDoc}
       */
      @Override
      public void registerAppController(CommunicationApplicationController controller)
      {
        //Store the application controller reference.
        MultiConnectionCommExchanger.this.appController = controller;
        
        // Set loop interactor.
        controller.setLoopInteractor(new ClientLoopInteractor());
      }

      @Override
      public void externalShutdownCommand()
      {

      }
    };

    // Create non-blocking comm server.
    this.nbcs = new NonBlockingCommunicationApplicationServer(Selector.open(), app, logger);

    // Run the nbcs.
    Thread thr = new Thread(() -> {
      nbcs.run();
    });
    thr.setDaemon(true);
    thr.start();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  //MultiConnectionCommExchanger controller.
  //
  //There will be one controller instance per exchanger instance and the controller will be used to send
  //commands to the exchanger.
  public interface Controller
  {
    /**
     * <p>
     * Request a listening connection on the given socket address. This address can
     * specify the port to listen on for all configured local IP addresses, or
     * additionally specify a local IP address to listen on. If the addr parameter
     * is null then a listening channel will be started on all local addresses using
     * an OS-selected port. The address and port of the actual listening connection
     * will be included in the
     * {@link CommExchangeEventListener#handlePassiveConnectionSuccess(ConnectionID, InetSocketAddress)}
     * callback method.
     * 
     * @param addr the socket address to bind the listening connection to.
     * @return connection ID associated with the listening channel connection <b>request</b>.
     */
    public ConnectionID requestListeningConnection(InetSocketAddress addr);
    
    /**
     * <p>
     * Send the outgoing connection data to the connection identified by the
     * connection ID, which must be a valid, open connection. Data will be
     * transferred from the specified write block handler into the connection's
     * outgoing send buffer
     * 
     * @param cid the connection ID of the open connection to send outgoing data on.
     * @param buffer the write block of data which will be copied into the connection's outgoing send buffer.
     */
    public void sendOutgoingConnectionData(ConnectionID cid, WriteBlockHandler buffer);
  }
  
  //Private controller implementation.
  private class ControllerImpl implements Controller
  {
    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionID requestListeningConnection(InetSocketAddress addr)
    {
      // Create a connection ID instance.
      ConnectionID cid = new ConnectionID();

      // Add the connection request.
      PassiveConnectionRequest pcr = new PassiveConnectionRequest(cid, addr);

      synchronized (threadLock)
      {
        // Add this to the list.
        connectionList.add(pcr);
      }

      //Nudge the NBCAS.  This will wake it up and it will process connection requests.
      nbcs.nudge();
      
      // Return the new connection id.
      return cid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendOutgoingConnectionData(ConnectionID cid, WriteBlockHandler buffer)
    {
      synchronized (threadLock)
      {
        // Get the connection manager corresponding to the given connection id.
        ConnectionManager cm = connectionMap.get(cid);

        if (cm == null) throw new IllegalArgumentException();

        // Write the data to the connection manager.
        cm.transferWriteData(buffer);
      }
    }
    
  }
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // NBCAS internals.
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Abstract connection request.  Will be implemented below, by an active connection request or a passive connection request.
   *  
   * @author jdf19
   *
   */
  interface ConnectionRequest
  {
    void doRequest();
  }

  /**
   * <p>Perform an active connection request.  The active connection request will be made inside the comms thread.
   * 
   * @author jdf19
   *
   */
  class ActiveConnectionRequest implements ConnectionRequest
  {
    private final InetSocketAddress connectAddress;

    private final ConnectionID connID;

    public ActiveConnectionRequest(ConnectionID connID, InetSocketAddress connectAddress, SocketChannelListener listener)
    {
      this.connID = connID;
      this.connectAddress = connectAddress;
    }

    /**
     * 
     */
    @Override
    public void doRequest()
    {
      appController.openClientSocket(connectAddress, new ActiveConnectionManager(connID));

      // Active connection notification.
      //listener.handleActiveConnectionSuccess(connID);
    }

  }

  class PassiveConnectionRequest implements ConnectionRequest
  {
    private final InetSocketAddress connectAddress;

    private final ConnectionID connID;

    public PassiveConnectionRequest(ConnectionID connID, InetSocketAddress connectAddress)
    {
      this.connID = connID;
      this.connectAddress = connectAddress;
    }

    @Override
    public void doRequest()
    {
      //
      try
      {
        appController.openServerSocket(connectAddress, new IncomingConnectionManager(connID));
      }
      catch (ChannelRegistrationException e)
      {
        listener.handlePassiveConnectionFailure(connID, e.getMessage());
      }
    }

  }

  /**
   * <p>Interact with the non-blocking comm server thread.  Always wait for network events, and send outgoing data on all channels at the start
   * of each comms loop.
   * 
   * @author jdf19
   *
   */
  private class ClientLoopInteractor implements CommLoopInteractor
  {
    @Override
    public boolean shouldWaitForNetworkEvent()
    {
      // Should wait for network event if there is nothing pending in the send queue.
      return true;
    }

    @Override
    public void handleLoopStart()
    {
      synchronized (threadLock)
      {
        //Perform any outstanding connection requests.
        for(ConnectionRequest creq : connectionList)
        {
          //Do the connection request.
          creq.doRequest();
        }
        
        //Clear out the connection requests now they have been done.
        connectionList.clear();
      }
      
      // Write any outgoing data.
      for (ConnectionManager cm : connectionMap.values())
      {
        cm.doWrite();
      }
    }

  }

  private class IncomingConnectionManager implements InitialisedServerSocketChannelListener
  {
    private final ConnectionID connID;

    //private ServerSocketChannelController controller;

    public IncomingConnectionManager(ConnectionID connID)
    {
      this.connID = connID;
    }

    @Override
    public void hasShut()
    {
      //
      listener.handleListenerClosed(connID);
    }

    @Override
    public void initController(ServerSocketChannelController controller)
    {
      //this.controller = controller;
    }

    @Override
    public SocketChannelListener handleIncomingConnection(SocketAddress socketAddress) throws ChannelCreateException
    {
      // Connected socket on given remote address.
      //
      // Create a connection ID instance for the connected socket.
      ConnectionID connid = new ConnectionID();

      // Notify owner of successful active connection.
      listener.handlePassiveConnectionSuccess(connid, (InetSocketAddress) socketAddress);

      // Return a ConnectionManager instance to the NBCAS to manage communication
      // events.
      return new ConnectionManager(connid);
    }
  }

  private class ActiveConnectionManager extends ConnectionManager implements ClientSocketChannelListener
  {

    public ActiveConnectionManager(ConnectionID connID)
    {
      super(connID);
    }

    @Override
    public SocketChannelListener handleConnectionSuccess(SocketAddress saRemote) throws ChannelCreateException
    {
      // Notify listener.
      listener.handleActiveConnectionSuccess(connID);

      // Return this callback object reference.
      return this;
    }

    @Override
    public void handleConnectionFailure(String failReason)
    {
      // Notify listener of failure.
      listener.handleActiveConnectionFailure(connID, failReason);
    }

  }

  private class ConnectionManager implements SocketChannelListener
  {
    protected final ConnectionID connID;

    private SocketChannelController controller;

    private final GeneralPipelineBuffer sendBuffer = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.nonDirectBufferFactory());

    private final GeneralPipelineBuffer receiveBuffer = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.nonDirectBufferFactory());

    public ConnectionManager(ConnectionID connID)
    {
      this.connID = connID;
    }

    public void transferWriteData(WriteBlockHandler buffer)
    {
      sendBuffer.produceFromHandler(buffer);
    }

    public void doWrite()
    {
      controller.writeOutgoingData(sendBuffer);
    }

    @Override
    public void handleReadStop()
    {
      // TODO Auto-generated method stub

    }

    @Override
    public void handleDataRead()
    {
      // Fill receive buffer with received data from socket.
      controller.fillBufferFromChannel(receiveBuffer);

      // Notify the listener.
      listener.handleIncomingConnectionData(connID);
    }

    @Override
    public void handleChannelWriteContinue()
    {
      //
      doWrite();
    }

    @Override
    public void handleServerLoopEnd()
    {
      //No reaction to this condition is required.
    }

    @Override
    public void hasShut()
    {
      listener.handleConnectionClosed(connID);
    }

    @Override
    public void initController(SocketChannelController controller)
    {
      this.controller = controller;
    }

    @Override
    public void isClosing()
    {
      listener.handleConnectionClosing(connID);
    }

  }
}
