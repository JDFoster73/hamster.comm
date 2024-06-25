package hamster.comm.itf.listener;

import hamster.comm.itf.controller.ReadableChannelController;
import hamster.comm.server.listener.ReadTransferHandler;

/**
 * <p>Implementors of the incoming channel listener are able to accept incoming
 * byte data.  Also, implementors will be required to handle <code>read stop</code> notifications.  These notifications
 * are called by the server if the channel handler did not respond to a {@link #handleDataRead()} call.  The implementation
 * will need to do a read operation ({@link ReadableChannelController#fillBufferFromChannel(hamster.comm.buffer.FillableChannelBuffer)}) before
 * the server will resume listening for incoming data on the channel.
 * 
 * @author jdf19
 *
 */
public interface IncomingChannelListener
{
  /**
   * <p>The underlying mechanism has data to consume, but data were not consumed in response to a {@link #handleDataRead(ReadTransferHandler)} call.
   * The mechanism may spin so it will enter READ_STOP condition.  It must be restarted by invoking the controller's {@link ReadableChannelController#readRestart()}
   * method.
   */
  public void handleReadStop();
  
  /**
   * <p>Incoming data have been received on the channel.  Use {@link IncomingChannelListener#handleDataRead()} to transfer into a fillable buffer for
   * consumption.
   */
  public void handleDataRead();
  
}
