package hamster.comm.buffer.factory;

import java.nio.ByteBuffer;

/**
 * <p>This interface provides the base services for buffer factory objects.</p>
 * 
 * @author jdf19
 *
 */
public interface BufferFactory
{
  /**
   * <p>Initialise the buffer, which means to create the initial {@link ByteBuffer} instance.  If this instance has data written to it
   * such that these data outgrow the buffer's capacity, then that instance can be changed for another instance with the same data
   * but a larger capacity.</p>
   * 
   * @return initial {@link ByteBuffer} instance which can be expanded if required.
   */
  public ByteBuffer initialiseBuffer();
}
