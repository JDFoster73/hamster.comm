package hamster.comm.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Simple class for creating ByteBuffer instances which have specified capacity, order and directness.
 */
public class BufferFactory
{
  //The capacity to initialise buffer instances with.
  private int size;

  //The order to initialise buffer instances with.
  private ByteOrder order;

  //The directness to initialise buffer instances with.
  private boolean direct;

  /**
   * Private constructor - set a default value for the size and directness.
   *
   * @param initSize
   */
  private BufferFactory(int initSize, boolean direct)
  {
    this.size = initSize;
    this.direct = direct;
  }

  /**
   * Private no-args constructor so this class can't be instanced other than calling the static method.
   */
  private BufferFactory(int initSize)
  {
    this.size = initSize;
  }

  public static BufferFactory getDefaultBufferFactory()
  {
    return new BufferFactory(2000);
  }

  public BufferFactory configureOrder(ByteOrder order)
  {
    this.order = order;
    return this;
  }

  public BufferFactory setSize(int bytes)
  {
    this.size = bytes;
    return this;
  }

  public BufferFactory setDirectness(boolean directness)
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
