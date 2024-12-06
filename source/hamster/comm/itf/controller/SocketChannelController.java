package hamster.comm.itf.controller;

import java.nio.channels.SocketChannel;

/**
 * <p>The {@link SocketChannelController} interface allows control of an unerlying communication channel
 * by adding the ability to execute a graceful shutdown using {@link SocketChannelController#closeOutput()},
 * liberate the channel from its registered server and set and get socket options associated with the
 * channel. 
 *  
 * @author jdf19
 *
 */
public interface SocketChannelController extends BiDirectionalChannelController
{
  /**
   * <p>Initiate a graceful shutdown of the channel.  This will shut the write end down.  Data can still be
   * received on the channel.  When the remote end shuts down its output, the channel will be fully closed.
   */
  public void closeOutput();
  
  /**
   * <p>The owner of this controller instance can call the liberate method to
   * remove the socket channel from the NonBlockingCommunicationServer it is
   * registered to.  The channel will be completely deregistered and liberated.
   * <p>Any calls to this controller will throw runtime exceptions after than point.
   * 
   * @return the liberated socket channel.
   */
  public SocketChannel liberate();

  /**
   * <p>This method can be called from a different thread to the communication application thread.
   * It allows a connection to wake up the comm loop in response to a requirement to execute communication
   * actions, for example (and typically) to send data that have been produced by a different thread.</p>
   */
  public void nudge();

}
