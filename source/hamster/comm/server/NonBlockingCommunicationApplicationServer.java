package hamster.comm.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hamster.comm.communication.sockopts.SocketChannelOptionAccessor;
import hamster.comm.itf.listener.ClientSocketChannelListener;
import hamster.comm.itf.listener.DatagramChannelListener;
import hamster.comm.itf.listener.InitialisedServerSocketChannelListener;
import hamster.comm.itf.listener.SocketChannelListener;
import hamster.comm.itf.listener.UninitialisedServerSocketChannelListener;
import hamster.comm.server.exception.ChannelRegistrationException;
import hamster.comm.server.listener.CommunicationApplicationController;
import hamster.comm.server.listener.ReadTargetListener;
import hamster.comm.server.listener.WriteChannelHandler;
import hamster.comm.wakeupschedule.AsyncWakeupScheduler;
import hamster.comm.wakeupschedule.ProcessCallbackScheduler;
import hamster.comm.wakeupschedule.SyncWakeupScheduler;

/**
 * <p>The {@link NonBlockingCommunicationApplicationServer} class wraps a Java NIO selector loop in a {@link Runnable} object.  The user
 * provides a <b>communication application</b> to the server.  This communication application is implemented as an instance of the
 * {@link NonBlockingCommunicationApplicationImpl} interface.  As soon as the run() method is called, either by the client thread
 * invoking the run() method or the creation of a new Thread object specifying the instance as its runnable object, the server
 * will call the {@link NonBlockingCommunicationApplicationImpl} implementation provided and will register an application controller
 * with it.  This application controller can be used to create communication channels (server and client), perform limited interaction
 * with the server, and shut the server communication loop down.
 * <p>The communication application is called inside the comm server thread.  Any access to the implementation outside of the server
 * thread <b>must</b> be <code>synchronized</code>.
 * 
 * @author jdf19
 *
 */
public final class NonBlockingCommunicationApplicationServer implements Runnable
{
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // START OF NON-BLOCKING COMMUNICATION MANAGEMENT FIELDS.
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * Selector - non-blocking channels rely on this for the selection mechanism.
   */
  private final Selector selector;
  
  /**
   * Future process helper - allows scheduled wakeup calls for some time in the
   * future.
   */
  private final AsyncWakeupScheduler asyncScheduler;
  
  /**
   * Future process helper - allows scheduled wakeup calls for some time in the
   * future.
   */
  private final SyncWakeupScheduler syncScheduler;
  
  /**
   * <p>Logger which logs communication events.
   */
  private final Logger logger;
  
  /**
   * <p>
   * Dummy interactor by default. Any comm loop interactions will be routed to
   * this dummy instance and ignored.
   */
  private CommLoopInteractor commLoopInteractor = new DummyInteractor();
  
  /**
   * <p>This consumer receives the contents of the channel selection key map values.  For each attachment that
   * implements {@link ReadTargetListener} it will call the {@link ReadTargetListener#handleCommLoopEnd()} callback method.
   */
  private LoopEndConsumer loopEndConsumer = new LoopEndConsumer();
  
  /**
   * CLOSES THE THREAD LOOP WHEN TRUE.  Called from inside the application thread.
   */
  private volatile boolean internalClose = false;

  /**
   * Called from outside the application thread.  Notifies the application instance that an external shutdown is required.
   */
  private volatile boolean externalClose = false;

  /**
   * <p>The communications application that should run inside this thread.
   */
  private NonBlockingCommunicationApplicationImpl application;

  /**
   * Skip remaining reads on the current loop.
   */
  private boolean skipReads = false;

  /**
   * <p>Create an instance of the non blocking communication application server.  The communication application server uses a Selector which is passed into the constructor.
   * This is to prevent the constructor having to throw an exception.  The application server may be created in one thread and started in another so propagating the exception
   * would be lengthy and complex.
   * 
   * @param application the application to use in the server operation.
   * @param logger logger for debug statements.
   * @throws IOException if there was an underlying communications exception when setting up the server.
   * 
   */
  public NonBlockingCommunicationApplicationServer(NonBlockingCommunicationApplicationImpl application, Logger logger) throws IOException
  {
    //Application instance.
    if(application == null) throw new NullPointerException();
    this.application = application;

    // Open the selector for non-blocking communications.
    this.selector = Selector.open();
    
    // Create the future process helpers.
    this.asyncScheduler = new AsyncWakeupScheduler();
    this.syncScheduler = new SyncWakeupScheduler();
    
    // Logger.
    this.logger = logger;
  }
  
  /**
   * The main selection thread.
   */
  @Override
  public void run()
  {
    // Startup - no waiting for future processes on first loop.
    long nextWakeup = 0;
    
    //Create the application handler instance and initialise the application instance.
    application.registerAppController(new InternalComm());
    
    // This is a daemon thread. It will close automatically when the system closes.
    while (!internalClose)
    {
      //Process external shutdown required command.
      if(externalClose)
      {
        application.externalShutdownCommand();
        //Don't process again.
        externalClose = false;
      }

      //Unset the skip reads flag at the top of this loop.
      skipReads = false;

      // Wait for the next wakeup time.
      int readyKeys = 0;
      try
      {
        if (commLoopInteractor.shouldWaitForNetworkEvent())
        {
          readyKeys = nextWakeup > 0 ? selector.select(nextWakeup) : selector.selectNow();
        }
        else
        {
          readyKeys = selector.selectNow();
        }
      }
      catch (IOException ex)
      {
        // TODO handle this (?). A closed selector exception, or general i/o exception.
        // Neither recoverable (?).
        ex.printStackTrace();
      }
      
      // Tell the interactor to handle loop start.
      commLoopInteractor.handleLoopStart();
      
      // Retrieve the selected key set.
      Set<SelectionKey> selectedKeys = selector.selectedKeys();
      
      try
      {
        // Check for ready keys.
        if (readyKeys > 0)
        {
          
          for (SelectionKey key : selectedKeys)
          {
            // Only handle readable keys if we can process incoming data (i.e. the outgoing
            // buffers aren't full).
            if (key.isReadable())
            {
              //Process reads if the "skipReads" flag has not been set.
              if (!skipReads)
              {
                handleRead(key);
              }
            }
            if (key.isValid() && key.isWritable())
            {
              handleWrite(key);
            }
            if (key.isValid() && key.isAcceptable())
            {
              handleAccept(key);
            }
            if (key.isValid() && key.isConnectable())
            {
              handleConnect(key);
            }
          }
          
        }
        // SET UP ALL WRITABLE CHANNELS - I.E. CHANNELS WITH OUTGOING BUFFER DATA TO
        // SEND.
        setWritableChannels();
      }
      catch (Throwable t)
      {
        t.printStackTrace();
      }
      finally
      {
        // Clear the selected keys set.
        selectedKeys.clear();
      }
      
      try
      {
        // Calculate the next wakeup time depending on the schedulers.
        nextWakeup = Math.min(asyncScheduler.serviceObjects(), syncScheduler.serviceObjects());
        
      }
      catch (Throwable t)
      {
        LoggerFactory.getLogger("LOG_GENERAL").error("There has been an uncaught exception while processing future processing tasks.  The exception given was: " + t.getMessage());
      }
      
      // Loop end.  Call all of the channel attachments with the loop end notification.  The loop end notification can be useful for handlers which gather data to send over the course of
      //a comms loop iteration.  These data can be coalesced into a larger block before sending, which requires only one call to the underlying channel's write mechanism rather than
      //multiple calls.
      //Handlers can decide whether to implement the loop end notification or not.  If they do implement the notification then they can perform any once per loop tasks which may 
      //streamline their operation.
      this.selector.keys().forEach(loopEndConsumer);


    }
    try
    {
      // Finishing. Make sure we put in a final Selector.selectNow() operation so that
      // any closed channels can be processed correctly.
      selector.selectNow();
      
      // Close the selector.
      selector.close();
    }
    catch (IOException e)
    {
      // TODO log.
      e.printStackTrace();
    }
  }

  /**
   *
   */
  public void externalApplicationShutdownRequired()
  {
    //Set the volatile external shutdown flag.
    externalClose = true;

    //Nudge the selector so that the loop can process this external shutdown command.
    selector.wakeup();
  }

  /**
   * Scan the selector selection key attachments for outgoing channel handlers. If
   * any of these handlers has a buffer with outgoing data, then set the writable
   * operation on that handler.
   */
  private void setWritableChannels()
  {
    // All selector keys. If the attachment supports the OutgoingChannelHandler
    // interface, then tell it to set
    // its OP_WRITE interest if there are outgoing data to send.
    for (SelectionKey key : selector.keys())
    {
      // Set writable operation if the attachment supports this operation.
      if (key.isValid() && (key.attachment() instanceof WriteChannelHandler))
      {
        ((WriteChannelHandler) key.attachment()).doLoopEnd();
      }
    }
  }
  
  /**
   * Handle a writable key.
   * 
   * @param key
   * @throws IOException
   */
  private void handleWrite(SelectionKey key)
  {
    // Cast the attachment to outgoing channel handler, which all attachments
    // capable of OP_WRITE interest must support.
    WriteChannelHandler outgoingChannelHandler = (WriteChannelHandler) key.attachment();
    
    outgoingChannelHandler.handleChannelWriteableEvent();
  }
  
  /**
   * Handle a readable key.
   * 
   * @param key
   */
  private void handleRead(SelectionKey key)
  {
    // Cast the attachment to incoming channel handler, which all attachments
    // capable of OP_READ interest must support.
    ReadTargetListener incomingChannelHandler = (ReadTargetListener) key.attachment();
    
    // Handle the incoming message data.
    incomingChannelHandler.handleChannelReadableEvent();
  }
  
  /**
   * Handle an acceptable key.
   * 
   * @param key
   */
  private void handleAccept(SelectionKey key)
  {
    // Cast the attachment to incoming connection listening channel handler, which
    // all attachments
    // capable of OP_ACCEPT interest must support.
    InitialisedServerSocketChannelAcceptHandler cah = (InitialisedServerSocketChannelAcceptHandler) key.attachment();
    
    // Handle the incoming message data.
    cah.handleChannelAcceptEvent();
  }
  
  /**
   * Handle a connectable key.
   * 
   * @param key
   */
  private void handleConnect(SelectionKey key)
  {
    // Cast the attachment to incoming connection listening channel handler, which
    // all attachments
    // capable of OP_ACCEPT interest must support.
    SocketChannelConnectHandler incomingResponseHandler = (SocketChannelConnectHandler) key.attachment();
    
    // Handle the incoming message data.
    incomingResponseHandler.handleChannelConnectEvent();
  }
  
  /**
   * If the selector is waiting for an event, wake it up so that connectors can be
   * processed.
   */
  public synchronized void nudge()
  {
    this.selector.wakeup();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // COMMUNICATION LOOP CONTROL
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // COMMUNICATION SERVICES MECHANISM
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
  private class InternalComm implements CommunicationApplicationController
  {    
    @Override
    public void openDatagramSocket(InetSocketAddress localAddress, InetSocketAddress remoteAddress, DatagramChannelListener connectionListener, SocketChannelOptionAccessor... optionSetters)
    {
      try
      {
        // Open the socket channel in non-blocking mode and register with the selector.
        DatagramChannel dc = DatagramChannel.open();
        
        // Non-blocking mode.
        dc.configureBlocking(false);
        
        // Register for READ.
        SelectionKey sk = dc.register(selector, SelectionKey.OP_READ);
        
        // Start connection.
        dc.bind(localAddress);
        dc.connect(remoteAddress);
        
        //
        DatagramChannelReadWriteHandler cch = new DatagramChannelReadWriteHandler(sk, dc, connectionListener);
        
        // Attach the connect handler to the selection key.
        sk.attach(cch);
      }
      catch (IOException e)
      {
        // Finished - can't connect.
        connectionListener.handleChannelFailure(e.getMessage());
      }
      
    }
   
    @Override
    public void openClientSocket(InetSocketAddress address, ClientSocketChannelListener connectionListener, SocketChannelOptionAccessor... optionSetters)
    {
      try
      {
        // Open the socket channel in non-blocking mode and register with the selector.
        SocketChannel sc = SocketChannel.open();
        
        // Non-blocking mode.
        sc.configureBlocking(false);
        
        // Register for CONNECT.
        SelectionKey sk = sc.register(selector, SelectionKey.OP_CONNECT);
        
        // Start connection.
        sc.connect(address);
        
        //
        SocketChannelConnectHandler cch = new SocketChannelConnectHandler(sk, sc, connectionListener, logger);
        
        // Attach the connect handler to the selection key.
        sk.attach(cch);
      }
      catch (IOException e)
      {
        // Finished - can't connect.
        connectionListener.handleConnectionFailure(e.getMessage());
      }
      
    }

    @Override
    public void registerOpenSocket(SocketChannel openChannel, SocketChannelListener listener)
    {
      try
      {
        // Put the channel into non-blocking mode and register with the selector.
        openChannel.configureBlocking(false);
        SelectionKey sk = openChannel.register(selector, SelectionKey.OP_READ);
        
        // Create the selection key attachment handler for handling read and write
        // events.
        SocketChannelReadWriteHandler srwch = new SocketChannelReadWriteHandler(sk, openChannel, listener, logger);
        
        // Attach the connect handler to the selection key.
        sk.attach(srwch);
        
        // Ready to communicate. Call init method of listener.
        listener.initController(srwch);
        
      }
      catch (IOException e)
      {
        // TODO LOGGIT.
        
        // Tell the listener that the channel has shut.
        listener.hasShut();
      }
      
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int openServerSocket(InetSocketAddress address, InitialisedServerSocketChannelListener connectionListener, SocketChannelOptionAccessor... optionSetters) throws ChannelRegistrationException
    {
      try
      {
        // Create listening channel.
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(address);
        
        // Set non-blocking mode.
        ssc.configureBlocking(false);
        
        // Register the channel.
        SelectionKey sk = ssc.register(selector, SelectionKey.OP_ACCEPT);
        
        // Selection key attachment / controller.
        InitialisedServerSocketChannelAcceptHandler sscah = new InitialisedServerSocketChannelAcceptHandler(ssc.getLocalAddress(), sk, connectionListener, logger);
        
        // Register the handler as the sk attachment.
        sk.attach(sscah);
        
        // Registered - init controller.
        connectionListener.initController(sscah);
        
        // Return the actual port number we bound to. If the server port was 0 in the
        // parameter address then the OS will
        // pick one. This can be determined in the server socket channel local address.
        int serverPort = ((InetSocketAddress) ssc.getLocalAddress()).getPort();
        return serverPort;
        
      }
      catch (IOException e)
      {
        // Finished - can't connect.
        throw new ChannelRegistrationException(e);
      }
      
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int openServerSocket(InetSocketAddress address, UninitialisedServerSocketChannelListener connectionListener, SocketChannelOptionAccessor... optionSetters) throws ChannelRegistrationException 
    {
      try
      {
        // Create listening channel.
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(address);
        
        // Set non-blocking mode.
        ssc.configureBlocking(false);
        
        // Register the channel.
        SelectionKey sk = ssc.register(selector, SelectionKey.OP_ACCEPT);
        
        // Selection key attachment / controller.
        UninitialisedServerSocketChannelAcceptHandler sscah = new UninitialisedServerSocketChannelAcceptHandler(ssc.getLocalAddress(), sk, connectionListener, logger);
        
        // Register the handler as the sk attachment.
        sk.attach(sscah);
        
        // Registered - init controller.
        connectionListener.initController(sscah);
        
        // Return the actual port number we bound to. If the server port was 0 in the
        // parameter address then the OS will
        // pick one. This can be determined in the server socket channel local address.
        int serverPort = ((InetSocketAddress) ssc.getLocalAddress()).getPort();
        return serverPort;
        
      }
      catch (IOException e)
      {
        // Finished - can't connect.
        throw new ChannelRegistrationException(e);
      }
      
    }

    @Override
    public CommLoopCloser getCommLoopCloser()
    {
      return () -> {
        internalClose = true;
      };
    }
    
    @Override
    public void setLoopInteractor(CommLoopInteractor interactor)
    {
      commLoopInteractor = interactor;
    }
    
    /**
    * This method allows access to the future process helper to be wrapped in a new
    * callback scheduler and returned to the caller. The caller can use this object
    * to schedule callbacks at future points in time up to 65s in the future.
    * 
    * @return
    */
    @Override
    public ProcessCallbackScheduler getProcessScheduler()
    {
      return new ProcessCallbackScheduler(asyncScheduler, syncScheduler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void skipRemainingIncomingThisLoop()
    {
      skipReads = true;
    }

    /**
     * <p>Wake up the comm loop selector.  This is a multithread safe operation so it can be called by a thread outside the main comm loop.  This is useful
     * for external threads which wish to pass commands to a connection handler instance and wake the selector up through the connection instance so that
     * the command can be actioned by the comm loop thread.
     */
    @Override
    public void nudge()
    {
      selector.wakeup();
    }
  }

  /**
   * <p>
   * Call each {@link ReadTargetListener}'s {@link ReadTargetListener#handleCommLoopEnd()} method.
   * 
   * @author jdf19
   *
   */
  class LoopEndConsumer implements Consumer<SelectionKey>
  {
    
    @Override
    public void accept(SelectionKey t)
    {
      if (t instanceof ReadTargetListener)
      {
        ((ReadTargetListener) t).handleCommLoopEnd();
      }
    }
    
  }
  
}
