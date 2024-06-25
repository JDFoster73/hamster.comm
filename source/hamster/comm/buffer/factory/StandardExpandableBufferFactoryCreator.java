package hamster.comm.buffer.factory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <p>
 * This class provides static methods for creating buffer factories with given
 * properties such as directness, initial size and order and growth multiplier.
 * </p>
 * 
 * @author jdf19
 *
 */
public class StandardExpandableBufferFactoryCreator
{
  /**
   * <p>
   * Return a standard non-direct buffer factory
   * </p>
   * 
   * @return expandable buffer factory which creates non direct buffers.
   */
  public static GrowUpdateExpandableBufferFactory nonDirectBufferFactory()
  {
    return new StandardInternalBufferFactory(false, 1000, null, 2);
  }

  /**
   * <p>
   * Return a standard non-direct buffer factory
   * </p>
   * 
   * @param bufSize the initial buffer size.
   * @return expandable buffer factory which creates non direct buffers of a given initial size.
   */
  public static GrowUpdateExpandableBufferFactory nonDirectBufferFactory(int bufSize)
  {
    return new StandardInternalBufferFactory(false, bufSize, null, 2);
  }

  /**
   * <p>
   * Return a standard direct buffer factory
   * </p>
   * 
   * @return expandable buffer factory which creates direct buffers.
   */
  public static GrowUpdateExpandableBufferFactory directBufferFactory()
  {
    return new StandardInternalBufferFactory(false, 1000, null, 2);
  }

  /**
   * <p>
   * Return a standard direct buffer factory
   * </p>
   * 
   * @param bufSize the initial buffer size.
   * @return expandable buffer factory which creates direct buffers of a given
   *         initial size.
   */
  public static GrowUpdateExpandableBufferFactory directBufferFactory(int bufSize)
  {
    // TODO!!!!!!!!! NON-DIRECT BUFFERS FOR TESTING ONLY!!!!! PUT THIS BACK!!!!!!!
    // VVV
    return new StandardInternalBufferFactory(false, bufSize, null, 2);
  }

  /**
   * <p>
   * Return a custom buffer factory.
   * 
   * @param directBuffers   true if factory should create direct buffers.
   * @param bufferInitSize  initial size of buffers that are created.
   * @param bufferInitOrder initial byte order of buffers that are created.
   * @param growMultiplier  the buffer grow multiplier - the old buffer size is
   *                        multiplied by this to get the resized buffer size.
   * @return the buffer factory.
   */
  public static ExpandableBufferFactory customBufferFactory(boolean directBuffers, int bufferInitSize, ByteOrder bufferInitOrder, float growMultiplier)
  {
    return new StandardInternalBufferFactory(directBuffers, bufferInitSize, bufferInitOrder, growMultiplier);
  }

  /**
   * <p>
   * Implementation of a growable buffer factory.
   * 
   * @author jdf19
   *
   */
  private static class StandardInternalBufferFactory implements GrowUpdateExpandableBufferFactory
  {
    /**
     * Direct buffer flag.
     */
    private boolean directBuffers;

    /**
     * Initial size.
     */
    private int bufferInitSize;

    /**
     * Initial byte order.
     */
    private ByteOrder bufferInitOrder;

    /**
     * <p>
     * Grow multiplier.
     */
    private float growMultiplier;

    /**
     * <p>
     * Create the buffer factory.
     * 
     * @param directBuffers   true if factory should create direct buffers.
     * @param bufferInitSize  initial size of buffers that are created.
     * @param bufferInitOrder initial byte order of buffers that are created.
     * @param growMultiplier  the buffer grow multiplier - the old buffer size is
     *                        multiplied by this to get the resized buffer size.
     */
    private StandardInternalBufferFactory(boolean directBuffers, int bufferInitSize, ByteOrder bufferInitOrder, float growMultiplier)
    {
      this.directBuffers = directBuffers;
      this.bufferInitSize = bufferInitSize;
      this.bufferInitOrder = bufferInitOrder;
      this.growMultiplier = growMultiplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer replaceBuffer(ByteBuffer buffer, int minimumSize)
    {
      if (buffer == null)
      {
        return createBuffer(bufferInitSize);
      }
      else
      {
        return copyBuffer(buffer, createBuffer(Math.max(minimumSize, (int) (buffer.capacity() * growMultiplier))));
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer replaceBuffer(ByteBuffer buffer)
    {
      if (buffer == null)
      {
        return createBuffer(bufferInitSize);
      }
      else
      {
        return copyBuffer(buffer, createBuffer((int) (buffer.capacity() * growMultiplier)));
      }
    }

    /**
     * <p>Create the initial buffer.
     * 
     * @param bufferSize the size of the buffer to create.
     * @return the newly created byte buffer.
     */
    private ByteBuffer createBuffer(int bufferSize)
    {
      if (directBuffers)
      {
        ByteBuffer buf = ByteBuffer.allocateDirect(bufferSize);
        if (bufferInitOrder != null) buf.order(bufferInitOrder);
        return buf;
      }
      else
      {
        ByteBuffer buf = ByteBuffer.allocate(bufferSize);
        if (bufferInitOrder != null) buf.order(bufferInitOrder);
        return buf;
      }
    }

    /**
     * <p>Copy the source buffer data into the target buffer.
     * 
     * @param sourceBuffer the buffer to copy data from.
     * @param targetBuffer the buffer to copy data to.
     * @return the target buffer reference.
     */
    private ByteBuffer copyBuffer(ByteBuffer sourceBuffer, ByteBuffer targetBuffer)
    {
      // Ensure the target buffer is clear.
      targetBuffer.clear();

      // Flip the source buffer.
      sourceBuffer.flip();

      // Copy job lot to target.
      targetBuffer.put(sourceBuffer);

      // Return the buffer that's having the data copied into it.
      return targetBuffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer initialiseBuffer()
    {
      return createBuffer(bufferInitSize);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GrowUpdateExpandableBufferFactory setGrowMultiplier(float growMultiplier)
    {
      // Set the multiplier.
      this.growMultiplier = growMultiplier;

      //
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GrowUpdateExpandableBufferFactory setInitialBufferSize(int bufferInitSize)
    {
      // Set the initial .
      this.bufferInitSize = bufferInitSize;

      //
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GrowUpdateExpandableBufferFactory setDirect(boolean direct)
    {
      this.directBuffers = direct;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GrowUpdateExpandableBufferFactory setInitialByteOrder(ByteOrder order)
    {
      this.bufferInitOrder = order;
      return this;
    }
  }
}
