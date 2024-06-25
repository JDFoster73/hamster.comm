package hamster.comm.buffer.durable;

import java.nio.channels.SocketChannel;

import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.buffer.FillableChannelBuffer;
import hamster.comm.itf.controller.SocketChannelController;
import hamster.comm.itf.listener.SocketChannelListener;
import hamster.comm.server.NonBlockingCommunicationApplicationServer;
import hamster.comm.server.listener.ReadTransferHandler;

/**
 * <p>This durable, bidirectional pipe session class forms an abstract wrapper around a durable communication session base,
 * extending it to use a Socket channel as the communication mechanism.
 * <p>This provides a listener for the outgoing channel and the incoming channel, and allows network events specific to a
 * {@link SocketChannelListener} instance to map to the underlying session.
 * <p>A superclass must implement the {@link #handleChannelEvent(CHANNEL_EVENT event)} method.  This will be called if either the source
 * or sink pipe are closed.  The superclass will get a {@link SocketChannel} or individual source and sink pipe
 * and register them with a {@link NonBlockingCommunicationApplicationServer}.
 * 
 * @author jdf19
 *
 */
public abstract class DurableBufferSocketChannelAdapter implements SocketChannelListener, DurableSessionSender, ReadTransferHandler
{
  /**
   * <p>Enumeration of channel events.  Subclasses can catch these events via the {@link #handleChannelEvent(CHANNEL_EVENT event)} method.
   * 
   * @author jdf19
   *
   */
  public enum CHANNEL_EVENT
  {
    /**
     * <p>The channel underlying the durable session has disconnected.
     */
    HAS_DISCONNECTED,

    /**
     * <p>The channel underlying the durable session has connected.
     */
    HAS_CONNECTED,
    ;
  }
  
  /**
   * <p>Channel controller allows the sending of data to the connected channel and the ability to shut the connected channel down.
   */
  protected SocketChannelController channelController;
  
  /**
   * <p>The session controller manages incoming and outgoing data - wrapping session data in communication packets and unwrapping
   * incoming communication packets to provide session data.
   */
  private DurableBufferSessionController session;

  /**
   * <p>Set to true if the durable session wishes to receive loop end notifications.  It can choose to send an entire
   * message packet at this point for example, rather than calling the channel write method multiple times (which is
   * expensive).
   */
  private boolean loopEndNotification;
  
  /**
   * <p>Create an instance of the durable socket channel session endpoint.  This sends and receives communication data via a
   * socket channel.
   * 
   * @param session the session controller for the durable buffer.
   */
  public DurableBufferSocketChannelAdapter(DurableBufferSessionController session)
  {
    this.session = session;
  }

  /**
   * <p>A subclass will implement this method which is called when there is a channel event.  The
   * default implementation does nothing; the subclass can use it to respond to events.
   * 
   * @param event the channel event raised that should be handled by the concrete subclass implementation.
   */
  protected abstract void handleChannelEvent(CHANNEL_EVENT event);
  
  /**
   * <p>Called when the socket channel listener detects that the communication channel has been shut or
   * liberated.  This means that a new channel is required.
   */
  private void handleDisconnection()
  {
    //Set the controllers to null and call the handleNeedConnection() abstract method.
    channelController = null;
    
    //Call the subclass, which implements details of how to form a connection.
    handleChannelEvent(CHANNEL_EVENT.HAS_DISCONNECTED);
  }

  /**
   * <p>Delegate incoming communication data to the underlying superclass session.
   */
  @Override
  public void handleDataRead()
  {
    session.handleCommunicationDataRead(this);
  }
//
  /**
   * <p>Data which the session wished to send but did not have buffer space to write into
   * can now be written.  Tell the session to attempt resend pending data.
   */
  @Override
  public void handleChannelWriteContinue()
  {
    session.continueSending();
  }

  /**
   * <p>The socket channel has shut.  We need reconnection.
   */
  @Override
  public void hasShut()
  {
    //Handle disconnection.
    handleDisconnection();
  }

  /**
   * <p>Called when the channel is registered.  Use the controller to write outgoing data.
   */
  @Override
  public void initController(SocketChannelController controller)
  {
    channelController = controller;
    
    //Connected and ready to communicate.
    //Initialise session to use this connection.
    session.reinitialiseCommunicationState(this);

    // Notify the implementation that the session is reconnected.
    handleChannelEvent(CHANNEL_EVENT.HAS_CONNECTED);
  }

  /**
   * <p>The remote endpoint is gracefully closing the socket.  Always complete closing and
   * then go to reconnection.
   */
  @Override
  public void isClosing()
  {
    //Close down channel fully.
    channelController.closeOutput();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int sendOutgoingCommunicationData(DrainableChannelBuffer writer)
  {
    //Send data to write controller if connected.
    if(channelController != null) return channelController.writeOutgoingData(writer);
    
    //Not connected - no bytes written.
    return 0;
  }
  
  /**
   * <p>Transfer as many bytes as possible into the given buffer argument, possibly filling it.</p>
   * 
   * @param reader the buffer to transfer bytes into.
   * @return the number of bytes transferred into the buffer.
   */
  public int transferChannelReadData(FillableChannelBuffer reader)
  {
    return channelController.fillBufferFromChannel(reader);
  }

  /**
   * <p>Transfer UPTO maxBytes bytes into the given buffer argument, possibly filling it.</p>
   * 
   * @param reader the buffer to transfer bytes into.
   * @param maxBytes the maximum number of bytes to transfer - may be less but will not be more.
   * @return the number of bytes transferred into the buffer.
   */
  public int transferChannelReadData(FillableChannelBuffer reader, int maxBytes)
  {
    return channelController.fillBufferFromChannel(reader, maxBytes);
  }

  /**
   * {@inheritDoc}
   */
  public final void setOutgoingLoopEndNotification(boolean notificationOn)
  {
    loopEndNotification = notificationOn;
  }

  public void handleServerLoopEnd()
  {
    if (loopEndNotification) session.handleLoopEnd();
  }
}
