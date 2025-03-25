package hamster.comm.itf.controller;

/**
 * <p>The {@link DatagramChannelController} interface allows control of an unerlying communication channel
 * by adding the ability to execute a graceful shutdown using {@link DatagramChannelController#closeOutput()},
 * liberate the channel from its registered server and set and get socket options associated with the
 * channel. 
 *  
 * @author jdf19
 *
 */
public interface DatagramChannelController extends ReadableChannelController, WritableChannelController
{
  /**
   * <p>Shut down the channel.  The channel closes immediately and cannot be used again.
   */
  public void close();
}
