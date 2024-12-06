/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.buffer.pipeline;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <p>This sequential buffer writer allows buffer data to be produced.  Underlying storage is implemented
 * in a {@link ByteBuffer} instance.
 * <p>This scheme attempts to simplify the use of the standard Java {@link ByteBuffer}.  The typical
 * useage of this buffer is:<br>
 * ...<br>
 * ... write data to the buffer ...<br>
 * ...<br>
 * ... write data to the buffer ...<br>
 * flip the buffer for sending<br>
 * write buffer data out to a channel<br>
 * compact the buffer for filling<br>
 * <br>
 * Splitting the ByteBuffer functionality into produce and send is an enforcement of the general usage
 * pattern of the ByteBuffer: for writing, it will be in fill mode unless it is sending.  After sending,
 * it is immediately put back into fill mode again.
 * <p>
 * The disadvantage of this scheme is that buffers are not used for both reading and writing, requiring
 * more buffer objects and more space.  Because the design of the Hamster system is fully around a
 * single threaded model, messaging is stateful and so it vastly simplifies things to only use buffers
 * for sending or receiving data.  There are no conflicts arising from the timings of sending and receiving
 * because those operations use entirely different buffer space.
 * 
 * @author jdf19
 */
public final class SequentialBlockWriter
{  
  /**
   * The underlying byte buffer used for storing and processing the write data.
   */
  protected ByteBuffer thisBuffer;
    
  /**
   * <p>Block start pos.  If < 0, the writer is not initialised and must throw an exception if any of the methods are accessed.
   * If >= 0, it is the buffer start position of the message block.  Post processing navigation <b>MUST NOT</b> travel beyond this.
   */
  private int blockStartPos = -1;

  //private int blockLength;

  /**
   * Construct the sequential buffer writer with the given underlying byte buffer and listener.
   *
   */
  SequentialBlockWriter(ByteBuffer internalBuffer)
  {
    //Store rebuffering factory reference.
    this.thisBuffer = internalBuffer;
  }
  
  /**
   * <p>Start writing a block message at the current write position.
   *
   */
  void  startBlock()
  {
    blockStartPos = thisBuffer.position();
  }

  /**
   * <p>Advance the write position by the given relative position.  This position can be a negative number, as long
   * as it is within the bounds of the existing write block.
   * 
   * @param bytesToAdvance the number of bytes to advance the write position - can be negative.
   * @return this writer instance.
   */
  public SequentialBlockWriter advance(int bytesToAdvance)
  {
    thisBuffer.position(thisBuffer.position() + bytesToAdvance);

    return this;
  }

  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialBlockWriter produceFloat(float data)
  {
    thisBuffer.putFloat(data);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialBlockWriter produceDouble(double data)
  {
    thisBuffer.putDouble(data);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialBlockWriter produceLong(long data)
  {
    thisBuffer.putLong(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialBlockWriter produceInt(int data)
  {
    thisBuffer.putInt(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialBlockWriter produceShort(short data)
  {
    thisBuffer.putShort(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialBlockWriter produceChar(char data)
  {
    thisBuffer.putChar(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialBlockWriter produceByte(byte data)
  {
    thisBuffer.put(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialBlockWriter produceBytes(byte[] data)
  {
    thisBuffer.put(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialBlockWriter produceBytes(byte[] data, int start, int len)
  {
    thisBuffer.put(data, start, len);
    return this;
  }
  
  /**
   * <p>Produce a byte at the given message block position.
   * 
   * @param b the byte value to produce to the writer.
   * @param pos the buffer byte position to produce the byte value to.
   * @return this buffer writer instance reference.
   */
  public SequentialBlockWriter produceByteAt(byte b, int pos)
  {
    thisBuffer.put(this.blockStartPos + pos, b);
    return this;
  }
  
  /**
   * <p>Produce a short at the given message block byte position.
   * 
   * @param b the short value to produce to the writer.
   * @param pos the buffer byte position to produce the short value to.
   * @return  this buffer writer instance reference.
   */
  public SequentialBlockWriter produceShortAt(short b, int pos)
  {
    thisBuffer.putShort(this.blockStartPos + pos, b);
    return this;
  }

  /**
   * <p>Produce a char at the given message block byte position.
   * 
   * @param value the char value to produce to the writer.
   * @param pos the buffer byte position to produce the char value to.
   * @return this buffer writer instance reference.
   */
  public SequentialBlockWriter produceCharAt(char value, int pos)
  {
    thisBuffer.putChar(this.blockStartPos + pos, value);
    return this;
  }

  /**
   * <p>Produce a int at the given message block byte position.
   * 
   * @param value the int value to produce to the writer.
   * @param pos the buffer byte position to produce the int value to.
   * @return this buffer writer instance reference.
   */
  public SequentialBlockWriter produceIntAt(int value, int pos)
  {
    thisBuffer.putInt(this.blockStartPos + pos, value);
    return this;
  }

  /**
   * <p>Take the available data in the source buffer argument and transfer it to this writer's buffer.
   * 
   * @param sourceBuffer the ByteBuffer (must be flipped and ready to transfer the data) from which to transfer data from.  The data will be produced to this writer.
   */
  public int transferByteBufferContent(ByteBuffer sourceBuffer)
  {
    //Store the byte position to calculate the number of bytes transferred.  The number transferred should be the source buffer's remaining byte count.
    int startPos = thisBuffer.position();
    
    //Put the data from the source buffer.
    thisBuffer.put(sourceBuffer);
    
    //Return the new position minus start position - the number of bytes transferred.
    return thisBuffer.position() - startPos;
  }

  /**
   * Dump a (barely) human readable string with the hex representation of the buffer bytes.
   * 
   * @return dump string.
   */
  public String dump()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("SEND: ");
    
    for (int i = 0; i < thisBuffer.position(); i++)
    {
      sb.append(String.format("%1$02X ", thisBuffer.get(i)));
    }
    
    return sb.toString();
  }

  /**
   * Allow buffer owner to change the byte order as required.
   * 
   * @param order the byte order to use when producing data.
   */
  public void setOrder(ByteOrder order)
  {
    thisBuffer.order(order);
  }

  /**
   * @return String representation of underlying ByteBuffer.
   */
  public String toString()
  {
    return thisBuffer.toString();
  }
}
