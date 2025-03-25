package hamster.comm.server.listener;

import hamster.comm.server.NonBlockingCommunicationApplicationServer;

/**
 * <p>This interface is implemented by {@link NonBlockingCommunicationApplicationServer} adapters.  When a registered
 * channel is writable (OP_WRITE interest is ON) then the implementing instance will be called to
 * write data to the channel.
 * 
 * @author jdf19
 *
 */
public interface WriteChannelHandler
{
  /**
   * <p>Underlying channel is writable; write data to it if they are available.
   */
  void handleChannelWriteableEvent();
  
  /**
   * <p>Called at the end of a communication server loop, an attempt should be made to 
   * write collected buffer data to the target.
   */
  void doLoopEnd();
}
