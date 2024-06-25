package hamster.comm.buffer.block.itf;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import hamster.comm.buffer.DrainableChannelBuffer;

/**
 * <p>Interface amalgamates <code>drainable</code> and <code>consumable</code> behaviour.  The implementation has a 
 * @author jdf19
 *
 */
public interface ConsumeDrainMessageBlockProvider extends DrainableChannelBuffer
{
  /**
   * <p>Drain data to the given writable channel from the given block position with the maximum number of bytes given.
   * 
   * @param channel the writable channel destination to drain the data to.
   * @param fromPosition the byte position in the block to start consuming.
   * @param maxBytesToSend the maximum number of bytes to send.  If fewer bytes are available then fewer will be sent but never more.
   * @return the number of bytes drained.
   * @throws IOException raised if an underlying transfer operation failed.
   */
  public int drainBufferToChannelFromPos(WritableByteChannel channel, int fromPosition, int maxBytesToSend) throws IOException;

  /**
   * <p>Drain data to the given writable channel from the given block position.
   * 
   * @param channel the writable channel destination to drain the data to.
   * @param fromPosition the byte position in the block to start consuming.
   * @return the number of bytes drained.
   * @throws IOException raised if an underlying transfer operation failed.
   */
  public int drainBufferToChannelFromPos(WritableByteChannel channel, int fromPosition) throws IOException;
  

}
