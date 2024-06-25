package hamster.comm.buffer;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * <p>Implementors of this interface guarantee to provide drain services to a writable channel.  They have the
 * ability to supply the buffer with either a determinate or an indeterminate amount of data, upto the amount
 * of sendable data that they have.
 * 
 * @author jdf19
 *
 */
public interface DrainableChannelBuffer
{
  /**
   * <p>Indeterminate drain to buffer method.  Attempt to drain as much sendable data as are held in the buffer.
   * The actual amount drained will depend on this and how much data the {@link WritableByteChannel} parameter
   * can accept.
   * 
   * @param channel the channel to drain buffer data to.
   * @return the number of bytes drained.
   * @throws IOException if an IOException occurred while the given channel was attempting to transfer buffer data.
   */
  public int drainBufferToChannel(WritableByteChannel channel) throws IOException;
  
  /**
   * <p>Determinate drain to buffer method.  Attempt to drain <b>up to</b> the number of bytes specified in the parameter
   * from the buffer during the call or all available data in the buffer if less than the maximum number of bytes specified.  
   * The actual amount drained will depend on this, how many bytes are in the buffer and how much data the 
   * {@link WritableByteChannel} parameter can accept.
   * 
   * @param channel the channel to drain buffer data to.
   * @param maxBytesToSend the maximum number of bytes that can be drained from the buffer in the call.
   * @return the number of bytes drained.
   * @throws IOException if an IOException occurred while the given channel was attempting to transfer buffer data.
   */
  public int drainBufferToChannel(WritableByteChannel channel, int maxBytesToSend) throws IOException;
  
  /**
   * <p>The implementation returns true if there are data available to send.
   * 
   * @return true if there are bytes in the buffer available to drain to a channel.
   */
  public boolean hasDataToConsume();
}
