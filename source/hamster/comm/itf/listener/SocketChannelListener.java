package hamster.comm.itf.listener;

import hamster.comm.communication.sockopts.SocketChannelOptionAccessor;
import hamster.comm.itf.controller.SocketChannelController;

/**
 * <p>When registering a socket channel in a communication server, an implementation of this interface
 * will be provided by the caller so that channel events can be responded to.
 * 
 * @author jdf19
 *
 */
public interface SocketChannelListener extends BiDirectionalChannelListener, BaseChannelListener
{
  /**
   * <p>Called by the NonBlockingCommunicationServer instance the channel is registered to
   * when all setup is complete and the channel is ready for communicating.
   * 
   * @param controller the controller to control the channel.
   */
  public void initController(SocketChannelController controller);  
  
  /**
   * <p>Called by the NonBlockingCommunicationServer instance the channel is registered to
   * when the remote end has been closed for output.  This indicates a graceful shutdown
   * is in progress.  Data can still be sent with the corresponding {@link SocketChannelController#writeOutgoingData(hamster.comm.buffers.block.ChannelTargetBufferWriter)} method,
   * and once finished the {@link SocketChannelController#closeOutput()} method can be called to complete the shutdown. 
   */
  public void isClosing();
  
  /**
   * <p>Get channel setup options.
   *  
   * @return socket channel option accessor.
   */
  public default SocketChannelOptionAccessor[] getSetupOptions()
  {
    return new SocketChannelOptionAccessor[] {};
  }
}
