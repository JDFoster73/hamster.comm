package hamster.comm.buffer.factory;

import java.nio.ByteOrder;

/**
 * <p>The {@link GrowUpdateExpandableBufferFactory} interface adds a layer of control onto {@link ExpandableBufferFactory}, allowing
 * the <code>grow multiplier</code>, <code>initial buffer size</code>, <code>directness</code> and <code>byte order</code> parameters 
 * to be updated for the buffer factory.</p>
 * @author jdf19
 *
 */
public interface GrowUpdateExpandableBufferFactory extends ExpandableBufferFactory
{
  /**
   * <p>Set the factor for expanding a buffer.  For example, if a buffer is 100 bytes and
   * the growth multiplier is 1.5 then the replacement buffer will have a capacity of 150 bytes.</p>
   * 
   * @param growMultiplier the growth multiplier.
   * @return reference to this instance.
   */
  public GrowUpdateExpandableBufferFactory setGrowMultiplier(float growMultiplier);
  
  /**
   * <p>Set the initial buffer size parameter.  Initial buffers that are created with the
   * {@link BufferFactory#initialiseBuffer()} method will have a capacity of this size.</p>
   * 
   * @param initSize the initial number of bytes for newly-initialised byte buffer instances.
   * @return reference to this instance.
   */
  public GrowUpdateExpandableBufferFactory setInitialBufferSize(int initSize);

  /**
   * <p>Set the initial buffer directness parameter.  Initial buffers that are created with the
   * {@link BufferFactory#initialiseBuffer()} method will have a directness of this type.</p>
   * 
   * @param direct true if the factory should create buffer objects which are backed by a <i>direct</i> ByteBuffer.
   * @return reference to this instance.
   */
  public GrowUpdateExpandableBufferFactory setDirect(boolean direct);

  /**
   * <p>Set the initial buffer order parameter.  Initial buffers that are created with the
   * {@link BufferFactory#initialiseBuffer()} method will have a byte order of this size.</p>
   * 
   * @param order this factory should create buffer objects which are backed by ByteBuffers with the given byte order.
   * @return reference to this instance.
   */
  public GrowUpdateExpandableBufferFactory setInitialByteOrder(ByteOrder order);
}
