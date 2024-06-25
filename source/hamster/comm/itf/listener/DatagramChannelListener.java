package hamster.comm.itf.listener;

import hamster.comm.communication.sockopts.SocketChannelOptionAccessor;
import hamster.comm.itf.controller.DatagramChannelController;
import hamster.comm.server.NonBlockingCommunicationApplicationServer;

/**
 * <p>When registering a datagram channel in a communication server, an implementation of this interface
 * will be provided by the caller so that channel events can be responded to.
 * 
 * @author jdf19
 *
 */
public interface DatagramChannelListener extends BiDirectionalChannelListener
{
  /**
   * <p>Called by the {@link NonBlockingCommunicationApplicationServer} instance the channel is registered to
   * when all setup is complete and the channel is ready for communicating.
   * 
   * @param controller the controller to control the channel.
   */
  public void initController(DatagramChannelController controller);  
    
  /**
   * <p>Get channel setup options.
   *  
   * @return socket channel option accessor.
   */
  public default SocketChannelOptionAccessor[] getSetupOptions()
  {
    return new SocketChannelOptionAccessor[] {};
  }

  /**
   * <p>If the channel creation failed for some reason then call this method to notify the owner.
   * 
   * @param message the exception message given by the OS.
   */
  public void handleChannelFailure(String message);

}
