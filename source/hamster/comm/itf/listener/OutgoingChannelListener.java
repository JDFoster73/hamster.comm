package hamster.comm.itf.listener;

import hamster.comm.server.NonBlockingCommunicationApplicationServer;

/**
 * <p>Implementors of the incoming channel listener are able to send outgoing
 * byte data.
 * 
 * @author jdf19
 *
 */
public interface OutgoingChannelListener
{
  /**
   * <p>Outgoing data have been sent on the channel.  Solid implementations
   * of this method are provided by users of the {@link NonBlockingCommunicationApplicationServer non-blocking communication server}.
   * The user-provided listener of the channel will be called with this method
   * when the outgoing channel is ready to send outstanding data.
   */
  public void handleChannelWriteContinue();
  
  /**
   * <p>Client-requested callback that the comm loop is at the end of scan.  Can be used to send data in one go.
   * <p>Default implementation is no-op.
   */
  public void handleServerLoopEnd();
}
