package hamster.comm.multithread;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;

import hamster.comm.buffer.block.GeneralPipelineBuffer;
import hamster.comm.buffer.factory.StandardExpandableBufferFactoryCreator;
import hamster.comm.ipc.blob.BLOBConsumer;
import hamster.comm.ipc.blob.BLOBManager;
import hamster.comm.ipc.blob.BLOBProducer;
import hamster.comm.itf.controller.SocketChannelController;
import hamster.comm.itf.listener.SocketChannelListener;
import hamster.comm.server.CommLoopInteractor;
import hamster.comm.server.NonBlockingCommunicationApplication;
import hamster.comm.server.NonBlockingCommunicationApplicationServer;
import hamster.comm.server.listener.CommunicationApplicationController;

/**
 * <p>This class provides support for multi-threaded clients to talk to a server channel.  A client may have (for example)
 * a UI thread and other worker threads which use a single communication channel.  This class runs a communication server
 * in its own thread.  That communication server will endevour to keep an open channel to a server socket.  Messages
 * to and from the server are exchanged via BLOB instances.
 * 
 * @author jdf19
 *
 */
public class MultiThreadCommExchanger
{
  /**
   * <p>
   * Non-blocking communication server for serving comms to the server.
   */
  private final NonBlockingCommunicationApplicationServer nbcs;
  
  /**
   * <p>
   * Outgoing blocking queue - thread safe for add and remove operations.
   */
  private final Queue<BLOBConsumer> outgoingQueue = new LinkedBlockingQueue<>();
  
  /**
   * <p>
   * Set when graceful shutdown is requested by local.
   */
  private final SyncPrimitive<Boolean> localCloseRequest = new SyncPrimitive<>(false);
  
  /**
   * <p>
   * Channel is closed status.
   */
  private final SyncPrimitive<Boolean> isShut = new SyncPrimitive<>(false);
  
  /**
   * <p>
   * Listener for receiving communication events. Events are delivered using the
   * NBCS thread.
   */
  private final CommExchangeIncomingListener commListener;
  
  /**
   * <p>
   * True if local shutdown is in progress.
   */
  private boolean localClosing;
  
  /**
   * <p>TODO
   * True if remote shutdown is in progress.
   */
  private boolean remoteClosing;
  
  /**
   * <p>
   * True if we are waiting for the socket to be writable.
   */
  private boolean sendWait;
  
  /**
   * <p>
   * Client communication listener.
   */
  private final ClientCommListener ccListener = new ClientCommListener();
  
  /**
   * <p>
   * Pipeline buffer for examining the message header.
   */
  private final GeneralPipelineBuffer headerReader;
  
  /**
   * <p>
   * The incoming BLOB instance that we are filling with incoming message data.
   * Once finished, the BLOB instance will be put on the queue and this reference
   * nullified.
   */
  private BLOBProducer incomingBLOB;
  
  /**
   * <p>
   * The length of the overall incoming message that is currently being assembled.
   */
  private int incomingMessageLen;
  
  /**
   * <p>
   * The outgoing BLOB instance that we are filling with outgoing message data.
   * Once finished, the BLOB instance will be put on the queue and this reference
   * nullified.
   */
  private BLOBConsumer outgoingBLOB;
  
  /**
   * <p>
   * The BLOB ID counter. It doesn't really convey any information as all BLOBs
   * are handled in order but necessary.
   */
  //private long blobID = 0;
  
  /**
   * <p>
   * Protocol descriptor allows the exchanger to determine complete messages.
   */
  private final CommExchangeProtocolDescriptor protocolDescriptor;
  
  /**
   * <p>
   * BLOB manager for acquiring BLOBProducers and then converting to BLOBConsumers
   * to add to incoming queue.
   */
  private final BLOBManager blobManager;
  
  /**
   * <p>Create the multi thread comm exchanger instance.
   * 
   * @param blobManager the BLOB manager to create BLOBs with in order to service exchange requests.
   * @param commListener communication listener is called back with comm events.
   * @param connectedChannel the socket channel.
   * @param protoDescriptor protocol descriptor.
   * @param logger logger for communication events.
   * @throws IOException thrown if an underlying exception occurs when creating.
   */
  public MultiThreadCommExchanger(BLOBManager blobManager, CommExchangeIncomingListener commListener, SocketChannel connectedChannel, CommExchangeProtocolDescriptor protoDescriptor, Logger logger) throws IOException
  {
    // BLOB manager.
    this.blobManager = blobManager;
    
    // Protocol descriptor.
    this.protocolDescriptor = protoDescriptor;
    
    //The communication application registers an open socket and sets the loop interactor.
    NonBlockingCommunicationApplication app = new NonBlockingCommunicationApplication()
    {
      
      /**
       * {@inheritDoc}
       */
      @Override
      public void registerAppController(CommunicationApplicationController controller)
      {
        // Set loop interactor.
        controller.setLoopInteractor(new ClientLoopInteractor());
        
        // Register established socket connection.
        controller.registerOpenSocket(connectedChannel, ccListener);
      }

      @Override
      public void externalShutdownCommand()
      {

      }
    };
    
    // Create non-blocking comm server.
    this.nbcs = new NonBlockingCommunicationApplicationServer(Selector.open(), app, logger);
        
    // Assign the listener.
    this.commListener = commListener;
    
    // Create header reader.
    headerReader = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.directBufferFactory(20), logger);
    
    // Run the nbcs.
    Thread thr = new Thread(() -> {
      nbcs.run();
    });
    thr.setDaemon(true);
    thr.start();
  }
  
  /**
   * <p>Send the outgoing blob communications.
   * 
   * @param bc the BLOB consumer to provide the data to send.
   */
  public void sendOutgoing(BLOBConsumer bc)
  {
    // Check the isClosing flag.
    if (localCloseRequest.getTheValue()) throw new IllegalStateException();
    
    // Add to the queue.
    if (!isShut.getTheValue()) outgoingQueue.add(bc);
    
    // Wake up the comm thread to deal with it.
    nbcs.nudge();
  }
  
  /**
   * <p>
   * Start closing the connection down. Any pending data will be sent but no new
   * data may be added to the queue.
   */
  public void startShutdown()
  {
    // Set the shutdown flag.
    localCloseRequest.setTheValue(true);
    
    // Nudge NBCS.
    nbcs.nudge();
  }
  
  /**
   * <p>
   * Implementation of the socket channel listener, the instance which interacts
   * with the nbcs.
   * 
   * @author jdf19
   *
   */
  private class ClientCommListener implements SocketChannelListener
  {
    /**
     * <p>
     * Channel controller.
     */
    private SocketChannelController controller;
    
    /**
     * <p>
     * Attempt to send. If we have an outstanding message then finish that and then
     * start the next. Repeat until outgoing data are exhausted or outgoing buffer
     * space is full.
     */
    private void doSend()
    {
      // Finished sending.
      boolean finished = false;
      
      while (!finished)
      {
        if (outgoingBLOB == null)
        {
          // Start a new message.
          if (!outgoingQueue.isEmpty())
          {
            // Get next outgoing message
            outgoingBLOB = outgoingQueue.remove();
          }
          else
          {
            // No BLOB to send. Break.
            break;
          }
        }
        
        // Attempt to send data.
        int send = controller.writeOutgoingData(outgoingBLOB);
        // If we didn't send any data then finish. The underlying mechanism sets
        // the OP_WRITE interest and calls continueSending().
        if (send == 0)
        {
          finished = true;
        }
        else
        {
          // Send finished?
          if (!outgoingBLOB.hasDataToConsume())
          {
            // Finish the BLOB.
            outgoingBLOB.finish();
            
            // Nullify the ougoing blob reference.
            outgoingBLOB = null;
          }
        }
      }
    }
    
    /**
     * <p>
     * Initiate graceful shutdown or close completely if other end has initiated
     * graceful shutdown.
     */
    private void beginShutdown()
    {
      controller.closeOutput();
    }
    
    @Override
    public void handleReadStop()
    {
      // Shouldn't happen?
    }
    
    /**
     * <p>
     * Build messages into BLOBProducer objects and then send them to the incoming
     * message queue.
     */
    @Override
    //removed 23/06/2022 general removal of transfer handler : public void handleDataRead(ReadTransferHandler transferHandler)
    public void handleDataRead()
    {
      // Finished flag.
      boolean finished = false;
      
      while (!finished)
      {
        // Is there an incoming BLOB?
        if (incomingBLOB == null)
        {
          // New message. Read the header and set the BLOB up to receive.
          //transferHandler.transferChannelReadData(headerReader, protocolDescriptor.queryProtocolHeaderSize() - headerReader.readableBytesInBuffer());
          controller.fillBufferFromChannel(headerReader, protocolDescriptor.queryProtocolHeaderSize() - headerReader.readableBytesInBuffer());
          
          // If the header is complete then we can determine the overall message length.
          if (headerReader.readableBytesInBuffer() >= protocolDescriptor.queryProtocolHeaderSize())
          {
            // Read the header.
            headerReader.consumeStartAllBufferData((r) -> {
              // Transfer all to blob.
              protocolDescriptor.readMessageBlock(r);
            });
          }
          else
          {
            // Not enough data to construct the header. Finish and receive more data.
            finished = true;
            continue;
          }
          
          // We've got header data; we can now set the BLOB up.
          // Get the incoming message length.
          incomingMessageLen = protocolDescriptor.queryMessageLen();
          
          System.out.println("MTCE receiving message of " + incomingMessageLen + " bytes.");
          
          // Get a BLOB for the message.
          incomingBLOB = blobManager.constructBLOB(incomingMessageLen);
          
          // Transfer the header to the BLOB.
          headerReader.consumeStartAllBufferData((r) -> {
            // Transfer all to blob.
            incomingBLOB.produceFromHandler(r);
          });
          
          //Messages can contain only header and no payload.  Check to see if this is the case.
          if(incomingMessageLen == incomingBLOB.blobDataPresent())
          {
            //Complete.  Handle this message.
            //
            //Notify incoming message listener.
            commListener.handleIncomingData(incomingBLOB.getConsumer());
            
            //Nullify incoming BLOB.
            incomingBLOB = null;
            
            //Set incoming blob len to 0.
            incomingMessageLen = 0;
          }
        }
        else
        {
          //We have the BLOB with the header and possibly partial data in it.  Continue to build the BLOB until all expected data are consumed from
          //the network channel.

          //Continue to transfer into BLOB.
          int i = controller.fillBufferFromChannel(incomingBLOB, incomingMessageLen - incomingBLOB.blobDataPresent());
          
          //If we've received 0 then we need to wait for the channel to be readable again as we need more data to complete the BLOB.
          if(i == 0)
          {
            finished = true;
            //break;
          }
          else
          {
            //If we are finished then load the BLOB into the queue and try the next one.
            if(incomingBLOB.blobDataPresent() >= incomingMessageLen)
            {
              //Complete.
              //
              //Notify incoming message listener.
              commListener.handleIncomingData(incomingBLOB.getConsumer());//blobManager.retrieveConsumer(blobID++));
              
              //Nullify incoming BLOB.
              incomingBLOB = null;
              
              //Set incoming blob len to 0.
              incomingMessageLen = 0;
            }
          }
        }
      }
    }
    
    /**
     * <p>
     * Outgoing data to send but outgoing buffer was full. Continue writing.
     */
    @Override
    public void handleChannelWriteContinue()
    {
      // Continue writing to channel.
      ccListener.doSend();
    }
    
    @Override
    public void handleServerLoopEnd()
    {
      // Not implemented.
    }
    
    @Override
    public void hasShut()
    {
      // Notify listener.
      commListener.handleChannelShut();
    }
    
    /**
     * <p>
     * Store the reference to the channel controller.
     */
    @Override
    public void initController(SocketChannelController controller)
    {
      this.controller = controller;
    }
    
    /**
     * <p>
     * Channel is closing. Notify.
     */
    @Override
    public void isClosing()
    {
      // Set remote closing flag.
      remoteClosing = true;
      
      commListener.handleChannelClosing();
    }
  }
  
  private class ClientLoopInteractor implements CommLoopInteractor
  {
    @Override
    public boolean shouldWaitForNetworkEvent()
    {
      // Should wait for network event if there is nothing pending in the send queue.
      return sendWait || outgoingQueue.isEmpty();
    }
    
    @Override
    public void handleLoopStart()
    {
      // Check for local shutdown.
      if (localCloseRequest.getTheValue() && !localClosing) ccListener.beginShutdown();
      
      // Check for outgoing message data to send.
      if (!sendWait && !outgoingQueue.isEmpty()) ccListener.doSend();
    }
    
  }
  
  /**
   * <p>
   * Synchronize access on a primitive value.
   * 
   * @author jdf19
   *
   */
  private class SyncPrimitive<T>
  {
    /**
     * <p>
     * The value to synchronize access on.
     */
    private T _theValue;
    
    public SyncPrimitive(T _theValue)
    {
      this._theValue = _theValue;
    }
    
    public synchronized void setTheValue(T theValue)
    {
      _theValue = theValue;
    }
    
    public synchronized T getTheValue()
    {
      return _theValue;
    }
  }
}

