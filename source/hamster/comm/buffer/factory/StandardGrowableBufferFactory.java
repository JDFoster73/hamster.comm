package hamster.comm.buffer.factory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <p>Standard factory for creating growable length buffers.
 *  
 * @author jdf19
 *
 */
public class StandardGrowableBufferFactory implements BufferFactory
{
  /**
   * <p>True if the factory should create ByteBuffer instances with direct underlying buffers.
   */
  private boolean directBuffers;
  
  /**
   * <p>The initial size to create the buffer.
   */
  private int bufferInitSize;
  
  /**
   * <p>The byte order to initialise the buffer with.
   */
  private ByteOrder bufferInitOrder;

  /**
   * <p>Construct the buffer factory.
   * 
   * @param directBuffers buffers should be created with direct buffers.
   * @param bufferInitSize buffers should be initialised with this size.
   * @param bufferInitOrder buffers should be initialised with this byte order.
   */
  public StandardGrowableBufferFactory(boolean directBuffers, int bufferInitSize, ByteOrder bufferInitOrder)
  {
    this.directBuffers = directBuffers;
    this.bufferInitSize = bufferInitSize;
    this.bufferInitOrder = bufferInitOrder;
  }
  
  /**
   * <p>Set should create direct buffers flag.
   * 
   * @param isDirect true if buffers should be created with direct buffers.
   * @return this instance reference.
   */
  public StandardGrowableBufferFactory setDirect(boolean isDirect)
  {
    this.directBuffers = isDirect;
    return this;
  }

  /**
   * <p>Set initial buffer size.
   * 
   * @param initSize the initial buffer size.  Buffers will grow to accommodate larger data requirements. 
   * @return this instance reference.
   */
  public StandardGrowableBufferFactory setInitSize(int initSize)
  {
    this.bufferInitSize = initSize;
    return this;
  }

  /**
   * <p>Set initial buffer byte order.
   * 
   * @param initOrder the initial byte order.
   * @return this instance reference.
   */
  public StandardGrowableBufferFactory setDirect(ByteOrder initOrder)
  {
    this.bufferInitOrder = initOrder;
    return this;
  }

  /**
   * <p>Create a buffer.
   * 
   * @param bufferSize the buffer size.
   * @return the newly-created ByteBuffer instance.
   */
  private ByteBuffer createBuffer(int bufferSize)
  {
    if(directBuffers)
    {
      ByteBuffer buf = ByteBuffer.allocateDirect(bufferSize);
      if(bufferInitOrder != null) buf.order(bufferInitOrder);
      return buf;
    }
    else
    {
      ByteBuffer buf = ByteBuffer.allocate(bufferSize);
      if(bufferInitOrder != null) buf.order(bufferInitOrder);
      return buf;        
    }
  }

  /**
   * <p>Create an initial buffer.
   * 
   * @return the newly-created ByteBuffer instance.
   */
  @Override
  public ByteBuffer initialiseBuffer()
  {
    return createBuffer(bufferInitSize);
  }

}
