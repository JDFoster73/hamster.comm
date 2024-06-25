package hamster.comm.buffer.factory;

import java.nio.ByteBuffer;

/**
 * <p>Expandable buffer factory interface.  Implementations use the method {@link ExpandableBufferFactory#replaceBuffer(ByteBuffer, int)} to
 * provide a replacement to the given {@link ByteBuffer} instance which has the same data but a larger capacity.</p>
 * 
 * @author jdf19
 *
 */
public interface ExpandableBufferFactory extends BufferFactory
{
  /**
   * <p>Replace the given buffer with a new one of <b>at least</b> the requested minimum size.
   * 
   * @param buffer the buffer to replace.
   * @param minimumSize the minimum number of bytes that the new buffer should be.
   * @return the replacement buffer containing the same data as the specified buffer to replace.
   */
  public ByteBuffer replaceBuffer(ByteBuffer buffer, int minimumSize);

  /**
   * <p>Replace the given buffer with a new one.  The size of the expansion is governed by the factory implementation.
   * 
   * @param buffer the buffer to replace.
   * @return the replacement buffer containing the same data as the specified buffer to replace.
   */
  public ByteBuffer replaceBuffer(ByteBuffer buffer);
}
