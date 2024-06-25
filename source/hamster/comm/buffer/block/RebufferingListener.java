package hamster.comm.buffer.block;

import java.nio.ByteBuffer;

/**
 * <p>This is implemented by classes which may be rebuffered.  The implementation can update
 * the listener if it is rebuffered.
 * @author jdf19
 *
 */
public interface RebufferingListener
{
  /**
   * <p>Notify the listener that a rebuffering operation has taken place, and supply the
   * new buffer instance.
   * 
   * @param newBuf the new buffer instance.
   */
  public void handleRebuffer(ByteBuffer newBuf);
}
