package hamster.comm.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;

import hamster.comm.KeyHelper;
import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.buffer.FillableChannelBuffer;
import hamster.comm.communication.sockopts.SocketChannelOptionAccessor;
import hamster.comm.itf.controller.BaseChannelOptionController;
import hamster.comm.itf.controller.SocketChannelController;
import hamster.comm.itf.listener.SocketChannelListener;
import hamster.comm.server.listener.ReadTargetListener;
import hamster.comm.server.listener.ReadTransferHandler;
import hamster.comm.server.listener.WriteChannelHandler;

/**
 * <p>
 * The socket read/write channel handler is the selector's plug-in handler for a
 * bi-directional socket channel. Instances of this class are set as selection
 * key attachments when registering a socket channel. It is called when the
 * selection key is in the following states:
 * <ul>
 * <li>OP_READ calls handleChannelReadableEvent()
 * <li>OP_WRITE calls handleChannelWritableEvent()
 * </ul>
 * <br>
 * through the {@link ReadTargetListener} and {@link WriteChannelHandler}
 * interface methods.
 * 
 * <p>
 * It also acts as the controller for the socket channel. The
 * {@link SocketChannelController} is an interface that allows the user to
 * control the channel (pause and resume reads, initiate close, shut down, write
 * channel data) without exposing any of the underlying channel and selector
 * mechanism.
 * 
 * <p>
 * Finally, the instance acts as a {@link ReadTransferHandler} which allows
 * incoming channel data to be read without exposing the selector or channel
 * mechanism.
 * 
 * @author jdf19
 *
 */
class SocketChannelReadWriteHandler implements ReadTargetListener, WriteChannelHandler, SocketChannelController, BaseChannelOptionController
{
  /**
   * Allow the communication thread selector to be nudged awake.
   */
  //private final NudgeController nudgeController;

  /**
   * The user-defined listener callback interface. It will be called with socket
   * channel events.
   */
  private SocketChannelListener channelListener;
  
  /**
   * Selection key for the channel. Used to set and clear interest operations.
   */
  private final SelectionKey selKey;
  
  /**
   * Channel object. Used for reading and writing data to the network channel.
   */
  private final SocketChannel channel;

  /**
   * Zero-read buffer.  If the channel owner consumes no data when called to handle a read event,
   * then use this buffer to do a read operation on the socket.  This will detect whether the read event
   * is due to the channel closing.
   */
  private final ByteBuffer zeroReadBuffer = ByteBuffer.allocate(0);

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
   * <p>Default to true for this flag.  The {@link SocketChannelReadWriteHandler} is instanced with an open channel.  Once either
   * the channel read end is shut down or the channel is completely shut then this flag will be set to false.
   */
  //private boolean isReadable = true;
  
  /**
   * Descriptor for logging. Useful to identify the channel's operations in the
   * log.
   */
  private final String desc;
  
  /**
   * <p>
   * Construct an instance of {@link SocketChannelReadWriteHandler}.
   * 
   * @param selKey          selection key for controlling interest ops.
   * @param channel         the channel for reading and writing data.
   * @param channelListener the listener which provides user callbacks for
   *                        handling channel events.
   * @param logger          the logger to use.
   */
  SocketChannelReadWriteHandler(SelectionKey selKey, SocketChannel channel, SocketChannelListener channelListener, Logger logger)
  {
    this.selKey = selKey;
    
    this.channel = channel;
    
    this.channelListener = channelListener;
    
    this.desc = channel.toString();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void handleChannelWriteableEvent()
  {
    channelListener.handleChannelWriteContinue();
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
    // Log this.
    // TODO
    
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

    //If drain op required not cleared then put channel into pause mode.  If the channel has closed then
    //the drainOpRequired flag will not have been reset so don't set the read mode to pause in this case.
    if(drainOpRequired && channel.isOpen())
    {
      //Zero read the channel to see if the channel has closed.
      int readLen = 0;
      try
      {
        readLen = channel.read(zeroReadBuffer);
      }
      catch (IOException e)
      {
        //Channel has had a hard close.  Handle the channel shutdown.
        handleWriteIOException(e);
      }

      //
      setToReadPauseMode();
    }
  }
  
  /**
   * Close the channel and cancel the selection key. Update the listener to tell
   * it that the channel has shut down.
   */
  private void handleChannelShutdown()
  {
    //The channel is shut down and is hence no longer readable.
    //isReadable = false;
    
    // Try to close the channel.
    try
    {
      channel.close();
    }
    catch (IOException e)
    {
      // TODO LOGGIT?
      // The channel is already useless and we can't do anything further.  Make sure that the key is deregistered and
      // any state associated with the channel is cleaned up.
    }
    
    // Make sure the registered key is cancelled.
    selKey.cancel();
    
    // Tell the channel owner that the channel has finished.
    channelListener.hasShut();
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
    // reading data from the channel to restart read events.
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
      //The input is shut down; the channel is no longer readable.
      //isReadable = false;
      
      // Call shutdown input.
      channel.shutdownInput();
      channel.socket().shutdownInput();
      
      // Is the output also shut down?
      if (channel.socket().isOutputShutdown())
      {
        handleChannelShutdown();
      }
      else
      {
        // Half close. Cancel readability of selection key and notify of input shutdown.
        KeyHelper.clearReadability(selKey);
        channelListener.isClosing();
      }
      
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
  public void closeOutput()
  {
    SocketChannel socketChannel = channel;
    
    // Close output.
    if (!socketChannel.socket().isOutputShutdown())
    {
      try
      {
        // Shut down the output of the socket channel.
        socketChannel.shutdownOutput();
        socketChannel.socket().shutdownOutput();
      }
      catch (IOException e)
      {
        handleChannelShutdown();
      }
    }
    
    // If the input is also shut down, close the channel and notify listeners.
    if (socketChannel.socket().isInputShutdown())
    {
      handleChannelShutdown();
    }
    
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void shut()
  {
    handleChannelShutdown();
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
  public SocketChannel liberate()
  {
    // Cancel the selection key which removes the selection key from the selector.
    selKey.cancel();
    
    // Return the channel object.
    return channel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void nudge()
  {
    selKey.selector().wakeup();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doLoopEnd()
  {
    // if(isLoopEndNotifyEnabled)
    {
      this.channelListener.handleServerLoopEnd();
    }
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
  public void handleCommLoopEnd()
  {
    // TODO Auto-generated method stub
    
  }
}
