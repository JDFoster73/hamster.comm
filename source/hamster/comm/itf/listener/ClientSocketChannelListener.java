package hamster.comm.itf.listener;

import java.net.SocketAddress;

import hamster.comm.communication.ChannelCreateException;

/**
 * <p>Client socket channel listener.  A connection request has been started and it wil either succeed or fail at some point in the future.
 * When the non-blocking operation completes, the channel manager instance implementing this interface will be called with success or
 * failure.  If success, then the channel manager instance must provide a solid implementation of the {@link SocketChannelListener}
 * interface that will be used to interact with the open socket channel.</p>
 * 
 * @author jdf19
 *
 */
public interface ClientSocketChannelListener
{
  /**
   * <p>Connection success notification.  Implementation must return a solid implementation of {@link SocketChannelListener}.</p>
   * @param saRemote the socket address of the remote peer that is connected to this channel.
   * @return Socket channel listener interface for reacting to socket events such as incoming data and socket close.
   * @throws ChannelCreateException if there was a problem with the underlying network provider when attempting to create the channel. 
   */
  public SocketChannelListener handleConnectionSuccess(SocketAddress saRemote) throws ChannelCreateException;
  
  /**
   * <p>The socket connection has failed.  The socket manager receives this and can react accordingly.</p>
   * 
   * @param failReason the reason for connection failure.
   */
  public void handleConnectionFailure(String failReason);
}
