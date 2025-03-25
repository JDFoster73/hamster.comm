package hamster.comm.server;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import hamster.comm.KeyHelper;
import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.buffer.FillableChannelBuffer;
import hamster.comm.communication.sockopts.SocketChannelOptionAccessor;
import hamster.comm.itf.controller.BaseChannelOptionController;
import hamster.comm.itf.controller.DatagramChannelController;
import hamster.comm.itf.listener.DatagramChannelListener;
import hamster.comm.itf.listener.SocketChannelListener;
import hamster.comm.server.listener.ReadTargetListener;
import hamster.comm.server.listener.WriteChannelHandler;

/**
 * <p>
 * The datagram read/write channel handler is the selector's plug-in handler for a
 * datagram channel. Instances of this class are set as selection
 * key attachments when registering a datagram channel. It is called when the
 * selection key is in the following states:
 * <ul>
 * <li>OP_READ calls handleChannelReadableEvent()
 * <li>OP_WRITE calls handleChannelWritableEvent()
 * </ul>
 * <br>
 * through the {@link ReadTargetListener} and {@link WriteChannelHandler}
 * interface methods.
 * 
 * <p>Datagrams work differently to streamed data.  Datagrams are either sent completely or not at all, so the 
 * {@link #writeOutgoingData(DrainableChannelBuffer)} or {@link #writeOutgoingData(DrainableChannelBuffer, int)} methods
 * should be called once per datagram.  If a call to either of these methods returns 0, the datagram could not be
 * written to the OS and the send buffer state will not change.  In this case, the OP_WRITE interest op will be
 * set and the {@link DatagramChannelListener#handleChannelWriteContinue()} method to notify the owner that the channel
 * is ready to accept more outgoing datagrams.
 * <p>Likewise, one complete datagram will be received for each {@link #fillBufferFromChannel(FillableChannelBuffer)} or
 * {@link #fillBufferFromChannel(FillableChannelBuffer, int)} call.  Incoming datagrams must be read in good time after
 * they come in.  The OS may queue datagrams until the channel's buffer space runs out and then silently discard further
 * incoming datagrams until the owner reads queued datagrams from the channel and frees up OS buffer space for more
 * incoming datagrams to be received.
 * 
 * @author jdf19
 *
 */
class DatagramChannelReadWriteHandler implements ReadTargetListener, WriteChannelHandler, DatagramChannelController, BaseChannelOptionController
{
  /**
   * The user-defined listener callback interface. It will be called with socket
   * channel events.
   */
  private DatagramChannelListener channelListener;
  
  /**
   * Selection key for the channel. Used to set and clear interest operations.
   */
  private final SelectionKey selKey;
  
  /**
   * Channel object. Used for reading and writing data to the network channel.
   */
  private final DatagramChannel channel;
  
  /**
   * <p>
   * A drain operation is required.  The OP_READ interest op is set and the OS has readable
   * data on the channel.  If we don't drain the incoming data fully then every subsequent
   * {@link Selector#select()} operation will return immediately with OP_READ active on this
   * channel.  If the {@link #handleChannelReadableEvent()} method is called by the communication
   * manager then this flag will be set.  The owner callback {@link SocketChannelListener#handleDataRead()} will
   * be called.  If the owner calls {@link #fillBufferFromChannel(FillableChannelBuffer)} or {@link #fillBufferFromChannel(FillableChannelBuffer, int)} then
   * the drain op required flag will be cleared.  If the {@link SocketChannelListener#handleDataRead()} call returns and the drain op flag
   * is still set then the channel will be put into pause mode.  The {@link SocketChannelListener#handleReadStop()} callback will be called to notify
   * the owner that the channel is on read stop.  It will not be resumed (i.e. the OP_READ interest will not be set on the channel) until data
   * have been drained to a {@link FillableChannelBuffer} by calling either of the fill..() methods.
   */
  private boolean drainOpRequired;
    
  /**
   * Descriptor for logging. Useful to identify the channel's operations in the
   * log.
   */
  private final String desc;
  
  /**
   * <p>
   * Construct an instance of {@link DatagramChannelReadWriteHandler}.
   * 
   * @param selKey          selection key for controlling interest ops.
   * @param channel         the channel for reading and writing data.
   * @param channelListener the listener which provides user callbacks for
   *                        handling channel events.
   * @param logger          the logger to use.
   */
  DatagramChannelReadWriteHandler(SelectionKey selKey, DatagramChannel channel, DatagramChannelListener channelListener)
  {
    this.selKey = selKey;
    
    this.channel = channel;
    
    this.channelListener = channelListener;
    
    this.desc = channel.toString();
    
    //Update listener - controller.
    channelListener.initController(this);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void handleChannelWriteableEvent()
  {
  }
  
  /**
   * <p>
   * Handle an {@link IOException}. Log, shut the channel down.
   * 
   * @param e
   * @return
   */
  private int handleWriteIOException(IOException e)
  {
    //Handle shutdown.
    handleChannelShutdown();
    
    // Return -1.
    return -1;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void handleChannelReadableEvent()
  {
    //Set the drain op required flag.
    drainOpRequired = true;
    
    // Call the channel listener to handle the read.
    channelListener.handleDataRead();

    //If drain op required not cleared then put channel into pause mode.
    if(drainOpRequired)
    {
      setToReadPauseMode();
    }
  }
  
  /**
   * Close the channel and cancel the selection key. Update the listener to tell
   * it that the channel has shut down.
   */
  private void handleChannelShutdown()
  {
    // Try to close the channel.
    try
    {
      channel.close();
    }
    catch (IOException e)
    {
      // TODO LOGGIT?
    }
    
    // Make sure the registered key is cancelled.
    selKey.cancel();
  }
  
  private void setToReadPauseMode()
  {
    // Detect full buffer condition. If the buffer we are transferring to has no
    // more space then the selection mechanism
    // can enter a spin condition until data are consumed from the buffer. If this
    // is the case then remove the OP_READ
    // interest until data have been consumed.
    // Clear OP_READ.
    KeyHelper.clearReadability(selKey);
    
    // Call the channel listener's readStop() method. The owner is responsible for
    // calling the associated channel controller's readRestart() method.
    channelListener.handleReadStop();
  }
  
  /**
   * <p>Set the OP_READ interest on the channel.
   */
  private void readRestart()
  {
    //
    KeyHelper.setReadability(selKey);
  }

  /**
   * Handle the channel shutdown.
   * 
   * @param e exception to log.
   * @return 0 - number of bytes transferred.
   */
  private int handleReadIOException(IOException e)
  {
    // Log this.
    
    // Handle channel close - it's unusable.
    handleChannelShutdown();
    
    // Socket closed. Return 0.
    return 0;
  }
  
  /**
   * <p>
   * Handle the data read. Detect a graceful shutdown and update the client
   * listener accordingly.
   * 
   * @param i the result of the channel.read(...) call.
   * @return the filtered bytes read. -1 means the channel is shutting down.
   * @throws IOException if we had a problem shutting the channel down.
   */
  private int handleReadData(int i) throws IOException
  {
    // Check the return value.
    if (i == -1)
    {
      // Return -1. Channel closed.
      return -1;
    }
    
    // Finished - return the number of bytes read.
    return i;
  }
  
  /**
   * Descriptive string for this attachment key target instance.
   */
  public String toString()
  {
    return desc;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int writeOutgoingData(DrainableChannelBuffer writer)
  {
    try
    {
      // Take in the data from the channel.
      int i = writer.drainBufferToChannel(channel);
      
      // Check for outstanding data. If so, turn ON channel writable notifications.
      KeyHelper.updateWriteability(selKey, writer.hasDataToConsume());
      
      return i;
    }
    catch (IOException e)
    {
      return handleWriteIOException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int writeOutgoingData(DrainableChannelBuffer writer, int maxBytesToSend)
  {
    try
    {
      // Take in the data from the channel.
      int i = writer.drainBufferToChannel(channel, maxBytesToSend);
      
      // Check for outstanding data. If so, turn ON channel writable notifications.
      KeyHelper.updateWriteability(selKey, writer.hasDataToConsume());
      
      return i;
    }
    catch (IOException e)
    {
      return handleWriteIOException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doLoopEnd()
  {
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void setOption(SocketChannelOptionAccessor option)
  {
    try
    {
      option.setOption(channel);
    }
    catch (IOException e)
    {
      handleWriteIOException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void getOption(SocketChannelOptionAccessor option)
  {
    try
    {
      option.getOption(channel);
    }
    catch (IOException e)
    {
      handleWriteIOException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int fillBufferFromChannel(FillableChannelBuffer targetBuffer)
  {
    try
    {
      //Fill the buffer from the incoming channel buffer.
      int res = targetBuffer.fillBufferFromChannel(channel);
    
      //Handle the read data.
      res = handleReadData(res);
      
      //We have read some data into the target buffer.  Set OP_READ.
      if(res > 0)
      {
        //Reset drain op required.
        drainOpRequired = false;
        
        //Set channel to readable.
        readRestart();
      }
            
      //Return the number of bytes resulting from the fill operation.
      return res;
    }
    catch(IOException e)
    {
      return handleReadIOException(e);      
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int fillBufferFromChannel(FillableChannelBuffer targetBuffer, int maxBytesToFill)
  {
    try
    {
      //Fill the buffer from the incoming channel buffer.
      int res = targetBuffer.fillBufferFromChannel(channel, maxBytesToFill);
    
      //Handle the read data.
      res = handleReadData(res);
      
      //We have read some data into the target buffer.  Set OP_READ.
      if(res > 0)
      {
        //Reset drain op required.
        drainOpRequired = false;
        
        //Set channel to readable.
        readRestart();
      }
      
      //Return the number of bytes resulting from the fill operation.
      return res;
    }
    catch(IOException e)
    {
      return handleReadIOException(e);      
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close()
  {
    handleChannelShutdown();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleCommLoopEnd()
  {
    // TODO Auto-generated method stub
    
  }
}
