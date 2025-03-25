package hamster.comm.buffer;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * <p>Implementations of this interface provide <b>filling</b> behaviour.  This means that they accept data
 * from a readable byte channel and will fill themselves from it.
 * 
 * @author jdf19
 *
 */
public interface FillableChannelBuffer extends AcceptingChannelBuffer
{
  /**
   * <p>Transfer bytes from a stream to the buffer implementation.</p>
   * 
   * @param channel the channel to transfer the bytes from
   * @return then number of bytes transferred
   * @throws IOException if the transfer operation raised a lower-level exception
   */
  public int fillBufferFromChannel(ReadableByteChannel channel) throws IOException;

  /**
   * <p>Transfer bytes from a stream to the buffer implementation.</p>
   * 
   * @param channel the channel to transfer the bytes from
   * @param maxBytes the maximum number of bytes to transfer from the channel to this buffer implementation
   * @return then number of bytes transferred
   * @throws IOException if the transfer operation raised a lower-level exception
   */
  public int fillBufferFromChannel(ReadableByteChannel channel, int maxBytes) throws IOException;
}
