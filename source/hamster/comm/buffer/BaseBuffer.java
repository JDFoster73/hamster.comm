package hamster.comm.buffer;

import java.nio.ByteBuffer;

public abstract class BaseBuffer
{
  /**
   * <p>Buffer object that is wrapped by this utility class.
   */
  protected final ByteBuffer internalBuffer;

  protected BaseBuffer(BufferFactory bufferFact)
  {
    internalBuffer = bufferFact.newBuffer();
  }

}
