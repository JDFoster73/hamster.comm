package hamster.comm.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
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
import hamster.comm.server.listener.MainCommunicationController;
import hamster.comm.server.listener.ReadTargetListener;
import hamster.comm.server.listener.ResendCommunicationController;
import hamster.comm.server.listener.WriteChannelHandler;
import hamster.comm.wakeupschedule.AsyncWakeupScheduler;
import hamster.comm.wakeupschedule.ProcessCallbackScheduler;
import hamster.comm.wakeupschedule.SyncWakeupScheduler;

/**
 * <p>The {@link NonBlockingCommunicationServer} class wraps a Java NIO selector loop in a {@link Runnable} object.  An instance of the classs can be managed outside the communications thread, i.e. the
 * {@link MainCommunicationController} methods can be called from the owner thread and access will be synchronised internally.
 * <p>When initial setup is complete then the instance should have its run() method called.  This method then takes control of the calling thread in the selector thread loop and does not exit while the selector loop is running.  
 * 
 * 
 * @author jdf19
  @deprecated
 */
final class _NonBlockingCommunicationServer implements Runnable, MainCommunicationController, ResendCommunicationController, CommLoopCloseProvider, CommLoopNudgeRequester
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
   * This flag will be true if the current communication loop must stop. It will
   * be reset at the start of the next loop.
   */
  private boolean stopCurrent = false;
  
  private final Logger logger;
  
  private Thread workerThreadId;
  
  /**
   * False if the thread hasn't been started yet. Other threads are allowed to
   * register channels before the thread starts.
   */
  private boolean started = false;
  
  /**
   * <p>
   * Dummy interactor by default. Any comm loop interactions will be routed to
   * this dummy instance and ignored.
   */
  private CommLoopInteractor commLoopInteractor = new DummyInteractor();
  
  /**
   * <p>
   * Dummy reprocess consumer by default.
   */
  private ReprocessConsumer reprocessConsumer = new ReprocessConsumer();
  
  /**
   * The reprocess flag is set if the reprocessReceivedData(...) method has been
   * called.
   */
  private boolean reprocess = false;
  
  /**
   * CLOSES THE THREAD LOOP WHEN TRUE.
   */
  private boolean close = false;
  
  /**
   * Only initialise channels in the worker thread AFTER the NBCS has been
   * started. Pre-registered channels each have a Runnable added to this list
   * which will perform the controller initialisation immediately after the worker
   * thread has started up.
   */
  private List<Runnable> initChannelList = new ArrayList<>();
  
  /**
   * <p>Create a non-blocking comm server instance.
   * 
   * @deprecated
   * @param selector the selector to use.
   * @param logger the logger to log communication events to.
   */
  public _NonBlockingCommunicationServer(Selector selector, Logger logger)
  {
    // Open the selector for non-blocking communications.
    this.selector = selector;
    
    // Create the future process helpers.
    this.asyncScheduler = new AsyncWakeupScheduler();
    this.syncScheduler = new SyncWakeupScheduler();
    
    // Logger.
    this.logger = logger;
  }
  
  /**
   * <p>Set the loop interactor.
   * 
   * @param interactor instance for interacting with the comm loop.
   */
  public void setLoopInteractor(CommLoopInteractor interactor)
  {
    this.commLoopInteractor = interactor;
  }
  
  /**
   * If the selector is waiting for an event, wake it up so that connectors can be
   * processed.
   */
  @Override
  public synchronized void nudge()
  {
    this.selector.wakeup();
  }
  
  /**
   * The main selection thread.
   */
  @Override
  public void run()
  {
    // Start flag.
    started = true;
    
    // Store reference to worker thread.
    this.workerThreadId = Thread.currentThread();
    
    // Startup - no waiting for future processes on first loop.
    long nextWakeup = 0;
    
    // Initialise pre-registered channels.
    initChannelList.forEach((rn) -> {
      rn.run();
    });
    
    initChannelList.clear();
    
    // This is a daemon thread. It will close automatically when the system closes.
    while (!close)
    {
      // Reset the stop comm loop flag.
      stopCurrent = false;
      
      // Reprocess.
      if (reprocess)
      {
        reprocess = false;
        this.selector.keys().forEach(reprocessConsumer);
      }
      
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
              handleRead(key);
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
            if (stopCurrent)
            {
              break;
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
      
      // Loop end - call abstract notification method if we can still process.
      if (!stopCurrent)
      {
        // loopEnd();
      }
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
  
  @Override
  public CommLoopCloser getCommLoopCloser()
  {
    return () -> {
      close = true;
    };
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
   * The future process helper is driven by the communication loop, so it needs to
   * be at this level.
   * 
   * This method allows access to the future process helper to be wrapped in a new
   * callback scheduler and returned to the caller. The caller can use this object
   * to schedule callbacks at future points in time up to 65s in the future.
   * 
   * @return process callback scheduler instance, which can be used to schedule future wakeup calls. 
   */
  public ProcessCallbackScheduler getProcessScheduler()
  {
    return new ProcessCallbackScheduler(asyncScheduler, syncScheduler);
  }
  
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // COMMUNICATION LOOP CONTROL
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // COMMUNICATION SERVICES MECHANISM
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * End the current communication loop. The next loop will start again from the
   * top.
   */
  public void endCurrentCommsLoop()
  {
    this.stopCurrent = true;
  }
  
  @Override
  public void openClientSocket(InetSocketAddress address, ClientSocketChannelListener connectionListener, SocketChannelOptionAccessor... optionSetters) // throws ChannelRegistrationException
  {
    // Must be calling this from the communication thread.
    if ((started) && (Thread.currentThread() != workerThreadId))
    {
      throw new IllegalCallerException();
    }
    
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
    // Must be calling this from the communication thread.
    if ((started) && (Thread.currentThread() != workerThreadId))
    {
      throw new IllegalCallerException();
    }
    
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
  public int openServerSocket(InetSocketAddress address, InitialisedServerSocketChannelListener connectionListener, SocketChannelOptionAccessor... optionSetters) throws ChannelRegistrationException // , ChannelBufferCreator bufferCreator
  {
    // Must be calling this from the communication thread.
    if ((started) && (Thread.currentThread() != workerThreadId))
    {
      throw new IllegalCallerException();
    }
    
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
      throw new ChannelRegistrationException();
    }
    
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int openServerSocket(InetSocketAddress address, UninitialisedServerSocketChannelListener connectionListener, SocketChannelOptionAccessor... optionSetters) throws ChannelRegistrationException 
  {
    // Must be calling this from the communication thread.
    if ((started) && (Thread.currentThread() != workerThreadId))
    {
      throw new IllegalCallerException();
    }
    
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
      throw new ChannelRegistrationException();
    }
    
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reprocessReceivedData()
  {
    // Go through all incoming channels. If they have incoming data, tell them to
    // process
    // it.
    reprocess = true;
  }
  
  /**
   * <p>
   * Accept readable socket updates upon a reprocess operation.
   * 
   * @author jdf19
   *
   */
  class ReprocessConsumer implements Consumer<SelectionKey>
  {
    
    @Override
    public void accept(SelectionKey t)
    {
      if (t instanceof ReadTargetListener)
      {
        ((ReadTargetListener) t).handleChannelReadableEvent();
      }
    }
    
  }

  @Override
  public void openDatagramSocket(InetSocketAddress localAddress, InetSocketAddress remoteSAddress, DatagramChannelListener connectionListener, SocketChannelOptionAccessor... optionSetters)
  {
    // TODO Auto-generated method stub
    
  }
  
}

