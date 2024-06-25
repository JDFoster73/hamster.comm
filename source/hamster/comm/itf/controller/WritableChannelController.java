package hamster.comm.itf.controller;

import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.itf.listener.OutgoingChannelListener;
import hamster.comm.server.NonBlockingCommunicationApplicationServer;

/**
 * <p>The writable channel controller is the basis of all writable channel controllers which are provided by the
 *  {@link NonBlockingCommunicationApplicationServer non-blocking communication server} when a channel is registered
 *  to it.
 *  <p>All channels are available to write outgoing data.
 *  
 * @author jdf19
 *
 */
public interface WritableChannelController
{
  /**
   * <p>This method can be used to write outgoing data to the channel.  As the underlying channel is in
   * non-blocking mode, only enough data that can fit in the the OS-level outgoing buffer can be written.
   * 
   * <p>If not all of the writable data in the buffer could be sent by the underlying communication mechanism, there will be a request
   * for a signal when the underlying channel is writable again (there is more space in the internal OS buffer for outgoing data).
   * When the signal is received, the related listener will have its {@link OutgoingChannelListener#handleChannelWriteContinue()} method called so the
   * owner can write more data to the buffer by calling the {@link #writeOutgoingData(ChannelTargetBufferWriter)} method.  
   * This cycle would continue until no more data were added to the outgoing buffer.<br>
   * 
   * <p>This method can be called successively.  Eventually, all of the underlying OS buffer space will be used up and so calling
   * it further will have no effect.  In this case, data will be left in the outgoing write buffer, and the underlying mechanism will
   * call the {@link OutgoingChannelListener#handleChannelWriteContinue()} method of the associated channel listener when the channel 
   * can accept more data.
   * 
   * @param writer the writer to write data for. 
   * @return the number of bytes written.
   */
  public int writeOutgoingData(DrainableChannelBuffer writer);

  /**
   * <p>This method functions as the {@link #writeOutgoingData(DrainableChannelBuffer)} method above but will only send upto the requested number of bytes.
   * How much it does send is not guaranteed to be the specified maximum and no bytes at all could be written, but no more than the specified maximum will
   * be written by the operation.
   * 
   * @param writer the writer to write data for. 
   * @param maxBytesToSend the maximum number of bytes to drain from the writer.  If there are fewer drainable bytes in the buffer than this number then all of those bytes will be drained.
   * @return the number of bytes written.
   */
  public int writeOutgoingData(DrainableChannelBuffer writer, int maxBytesToSend);

  //TODO REMOVE
  /**
   * <p>Set or unset the comm loop end notification callback to the client.  If <code>true</code>, the client will be called
   * with an end-of-loop notification at the end of every comm loop.  This is useful for doing bulk writes at the end of loop
   * processing.
   * 
   * @param notify update the comm loop end notification for the comm server that this channel is registered to.  A value of true will mean that the corresponding 
   */
  //public void activateCommLoopEndNotification(boolean notify);
}
