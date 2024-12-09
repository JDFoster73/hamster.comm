/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.buffer.pipeline;

import hamster.comm.buffer.block.itf.WriteBlockHandler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A {@link SequentialBlockReader} instance models the read phase of a ByteBuffer-style object.  This object is loaded with
 * stream data and can then consume that data in the order that it was produced in.  For that purpose, the object
 * provides <code>consumeXXX()</code> methods to consume primitive data types.
 * 
 * <p>The {@link WriteBlockHandler} interface is also implemented for convenience.  The reader instance can be used to write all available
 * block data to the given writer.
 * 
 * @author jdf19
 */
public class SequentialBlockReader
{
  /**
   * The internal buffer.
   */
  ByteBuffer thisBuffer;
  
  /**
   * <p>String builder for deserialising character strings.
   */
  private final StringBuilder charStringBuilder = new StringBuilder();

  /**
   * Block start pointer.  Immediately prior to a block being consumed, this value will be set to the start index
   * of consumable data in the internal buffer.
   */
  private int blockStartPointer;

  /**
   * Create a <code>SequentialBufferReader</code> instance with the given underlying ByteBuffer.
   * 
   * @param incomingBuffer the underlying ByteBuffer object that the sequential buffer uses to store and access the stream data.
   */
  SequentialBlockReader(ByteBuffer incomingBuffer)
  {
    //Assign the parameter to the internal buffer.
    thisBuffer = incomingBuffer;
  }
  
  /**
   * Create a <code>SequentialBufferReader</code> instance.
   */
  SequentialBlockReader()
  {
    //Need an empty default constructor.
  }

  /**
   * <p>Start processing a message block.  Save the start position for calculation purposes.  The owning
   * caller will already have set up the buffer position and limit prior to calling this method.
   */
  SequentialBlockReader setBlock()
  {
    this.blockStartPointer = thisBuffer.position();
    return this;
  }

  /**
   * <p>Consume a <code>double</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>double</code> value consumed from the reader.
   */
  public double consumeDouble()
  {
    return thisBuffer.getDouble();
  }
  
  /**
   * <p>Consume a <code>float</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>float</code> value consumed from the reader.
   */
  public float consumeFloat()
  {
    return thisBuffer.getFloat();
  }
  
  /**
   * <p>Consume a <code>long</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>long</code> value consumed from the reader.
   */
  public long consumeLong()
  {
    return thisBuffer.getLong();
  }

  /**
   * <p>Consume an <code>int</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>int</code> value consumed from the reader.
   */
  public int consumeInt()
  {
    return thisBuffer.getInt();
  }

  /**
   * <p>Consume a <code>short</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>short</code> value consumed from the reader.
   */
  public short consumeShort()
  {
    return thisBuffer.getShort();
  }
  
  /**
   * <p>Consume a <code>char</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>char</code> value consumed from the reader.
   */
  public char consumeChar()
  {
    return thisBuffer.getChar();
  }

  /**
   * <p>Consume a <code>byte</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>byte</code> value consumed from the reader.
   */
  public byte consumeByte()
  {
    return thisBuffer.get();
  }

  /**
   * <p>Consume a string of bytes into a byte array, specifying the offset of the array start and how many bytes to consume.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @param by the byte buffer to consume bytes to.
   * @param offset the start index of the byte buffer to start consuming bytes to.
   * @param len the number of bytes to consume to the byte buffer.
   */
  public void consumeBytes(byte[] by, int offset, int len)
  {
    //Consume the data.
    thisBuffer.get(by, offset, len);
  }

  /**
   * <p>Consume an array of bytes from the reader into the byte array instance specified by the parameter.  Enough data must remain in the current block to fill the whole passed byte array or an exception will be thrown.
   * 
   * @param byteArray the byte buffer to consume bytes to.
   */
  public void consumeBytes(byte[] byteArray)
  {
    //Consume the data.
    thisBuffer.get(byteArray);
  }

  /**
   * <p>Consume an array of bytes from the reader into a newly created byte array instance.
   * 
   */
  public byte[] consumeBytes()
  {
    //Create byte array.
    byte[] byteArray = new byte[bytesRemainingInBlock()];
    
    //Consume the data.
    thisBuffer.get(byteArray);
    
    //Return the byte array.
    return byteArray;
  }

  /**
   * <p>Replace the byte that has <b>just been read</b> by
   * the one in the parameter.
   * 
   * @param by the byte value to replace the byte value at the current consume position.
   */
  public void replaceByte(byte by)
  {
    //Replace the byte with the new one.
    thisBuffer.put(by);
  }
  
  /**
   * <p>Replace the short that has <b>just been read</b> by
   * the one in the parameter.
   * 
   * @param by the short value to replace the byte value at the current consume position.
   */
  public void replaceShort(short by)
  {
    //Replace the byte with the new one.
    thisBuffer.putShort(by);
  }

  /**
   * <p>Replace the char that has <b>just been read</b> by
   * the one in the parameter.
   * 
   * @param by the char value to replace the byte value at the current consume position.
   */
  public void replaceChar(char by)
  {
    //Replace the byte with the new one.
    thisBuffer.putChar(by);
  }

  /**
   * <p>Replace the int that has <b>just been read</b> by
   * the one in the parameter.
   * 
   * @param by the int value to replace the byte value at the current consume position.
   */
  public void replaceInt(int by)
  {
    //Replace the byte with the new one.
    thisBuffer.putInt(by);
  }

  /**
   * <p>Replace the char that has <b>just been read</b> by
   * the one in the parameter.
   * 
   * @param by the long value to replace the byte value at the current consume position.
   */
  public void replaceLong(long by)
  {
    //Replace the byte with the new one.
    thisBuffer.putLong(by);
  }

  /**
   * <p>Instead of consuming, skip the given number of bytes.  Useful for skipping unused data from protocol headers etc.
   * 
   * @param bytes the number of bytes to skip.
   */
  public void skip(int bytes)
  {
    //Move the read position forward by the requested number of bytes.
    thisBuffer.position(thisBuffer.position() + bytes);
  }
  
  /**
   * <p>Set the byte order of the underlying buffer.
   * 
   * @param order the byte order.
   */
  public void setOrder(ByteOrder order)
  {
    thisBuffer.order(order);
  }
  
  /**
   * Simply call the toString() method of the underlying byte buffer.
   * 
   * @return String containing human-readable descriptive data for the underlying buffer.
   */
  @Override
  public String toString()
  {
    return thisBuffer.toString();
  }

  /**
   * This is the number of bytes remaining in the current block.  Any attempt to read beyond this will result in 
   * a runtime exception being thrown.
   * 
   * @return the number of readable bytes remaining.
   */
  public int bytesRemainingInBlock()
  {
    //Return buffer remaining bytes.  Limit has been set on the message block
    //so number of remaining bytes in block is simply the number of bytes between
    //the position and the limit of the buffer.
    return thisBuffer.remaining();
  }

  /**
   * This method will return true if at least one byte can be read from the buffer.
   *
   * @return the number of readable bytes remaining.
   */
  public boolean hasRemainingBytesInBlock()
  {
    //Return buffer remaining bytes.  Limit has been set on the message block
    //so number of remaining bytes in block is simply the number of bytes between
    //the position and the limit of the buffer.
    return thisBuffer.remaining() > 0;
  }

  /**
   * This gives a fixed value of the total number of bytes in the block being processed.
   *  
   *  @return the total number of bytes that have been available to read
   */
  public int bytesInBlock()
  {
    // The byte buffer has been set up with the position at the start of the message block and the limit at the
    // end of the message block.  Use these values to compute the return value.
    return thisBuffer.limit();// - this.blockStart;
  }

  /**
   * The current block position, i.e. the number of bytes that have been read from the currently processed
   * block so far.
   * 
   * @return the number of bytes that have been read so far.
   */
  public int currentBlockReadPosition()
  {
    // The byte buffer has been set up with the position at the start of the message block and the limit at the
    // end of the message block.  Use these values to compute the return value.
    return thisBuffer.position();// - this.blockStart;
  }

  /**
   * Return the number of consumed bytes.
   *
   * @return
   */
  public int numberOfConsumedBytes()
  {
    return thisBuffer.position() - blockStartPointer;
  }
}
