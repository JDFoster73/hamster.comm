package hamster.comm.server.listener;

import java.nio.channels.SocketChannel;

import hamster.comm.itf.listener.SocketChannelListener;

/**
 * <p>This communication controller is provided for the inclusion of channels which are already open into the communication server.
 * The caller provides the open channel and a listener to catch communication events from this channel.
 * 
 * @author jdf19
 *
 */
public interface EstablishedChannelCommunicationController
{
  /**
   * <p>Register the open and connected channel.
   * 
   * @param openChannel the open and connected channel.
   * @param listener the listener to catch communication events from the channel.
   */
  public void registerOpenSocket(SocketChannel openChannel, SocketChannelListener listener);
}
