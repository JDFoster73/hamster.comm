package hamster.comm.itf.listener;

import hamster.comm.server.NonBlockingCommunicationApplicationServer;

/**
 * <p>This is the absolute base interface for non-blocking communication server registered channel listeners.
 * <p>All listeners, regardless of which kind of communication channel they represent, inherit this interface.
 * The {@link NonBlockingCommunicationApplicationServer non-blocking communication server} calls these methods   
 * 
 * @author jdf19
 *
 */
public interface BaseChannelListener
{
  /**
   * The channel has shut down completely.  The channel is unusable for any further action.
   * No further events will be received and the associated controller interface will throw
   * an exception if used.
   */
  public void hasShut();
  
  /**
   * The channel has been liberated.  The channel object has been liberated and fully deregistered
   * from the {@link NonBlockingCommunicationServer non-blocking communication server} so no further
   * events will be received from it.  The associated controller interface will throw
   * an exception if used.
   */
  //public void hasLiberated();
}
