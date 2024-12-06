package hamster.comm.buffer.pipeline;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PipelineBufferFactory
{
  private int size;

  private ByteOrder order;

  private boolean direct;

  /**
   * Private constructor - set a default value for the size and directness.
   *
   * @param initSize
   */
  private PipelineBufferFactory(int initSize, boolean direct)
  {
    this.size = initSize;
    this.direct = direct;
  }

  /**
   * Private no-args constructor so this class can't be instanced other than calling the static method.
   */
  private PipelineBufferFactory(int initSize)
  {
    this.size = initSize;
  }

  public static PipelineBufferFactory getBufferFactory()
  {
    return new PipelineBufferFactory(2000);
  }

  public PipelineBufferFactory configureOrder(ByteOrder order)
  {
    this.order = order;
    return this;
  }

  public PipelineBufferFactory setSize(int bytes)
  {
    this.size = bytes;
    return this;
  }

  public PipelineBufferFactory setDirectness(boolean directness)
  {
    this.direct = directness;
    return this;
  }

  public ByteBuffer newBuffer()
  {

    ByteBuffer ret = (direct) ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
    ret.order(order);
    return ret;
  }
}
