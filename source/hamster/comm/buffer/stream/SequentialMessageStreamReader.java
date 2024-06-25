/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.buffer.stream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A {@link SequentialMessageStreamReader} instance models the read phase of a ByteBuffer-style object.  This object is loaded with
 * stream data and can then consume that data in the order that it was produced in.  For that purpose, the object
 * provides <code>consumeXXX()</code> methods to consume primitive data types.
 * 
 * @author jdf19
 */
public abstract class SequentialMessageStreamReader
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
   * Create a <code>SequentialBufferReader</code> instance with the given underlying ByteBuffer.
   * 
   * @param incomingBuffer the underlying ByteBuffer object that the sequential buffer uses to store and access the stream data.
   */
  SequentialMessageStreamReader(ByteBuffer incomingBuffer)
  {
    //Assign the parameter to the internal buffer.
    thisBuffer = incomingBuffer;
  }
  
  /**
   * Create a <code>SequentialBufferReader</code> instance.  The underlying buffer will be set with a subsequent call to {@link SequentialMessageStreamReader#updateBuffer(ByteBuffer)}.
   */
  SequentialMessageStreamReader()
  {
    //Need an empty default constructor.
  }
  
  /**
   * <p>Update the buffer instance if it has been rebuffered elsewhere.
   * 
   * @param buffer
   */
  void updateBuffer(ByteBuffer buffer)
  {
    this.thisBuffer = buffer;
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
   * <p>Consume a string of bytes into a whole byte array.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @param byteArray the byte buffer to consume bytes to.
   */
  public void consumeBytes(byte[] byteArray)
  {
    //Consume the data.
    thisBuffer.get(byteArray);
  }

  /**
   * <p>Consume a character string from the reader.
   * 
   * @return the string value from the reader.  This must have been produced with a corresponding {@link SequentialMessageStreamWriter#produceCharacterString(String)} instruction.
   */
  public String consumeCharString()
  {
    //Make sure the string builder is clear.
    this.charStringBuilder.setLength(0);
    
    //Get the string length.
    int len = thisBuffer.getChar();
    
    //Copy the chars into the given buffer.
    for(int i = 0; i < len; i++)
    {
      this.charStringBuilder.append(thisBuffer.getChar());
    }
    
    return this.charStringBuilder.toString();
  }
  
  /**
   * <p>Consume a byte string from the reader to the target.
   * 
   * @param target the byte string to consume the byte data to.  This must have been produced with a corresponding {@link SequentialMessageStreamWriter#produceByteString(byte[])} instruction.
   * @return the number of bytes consumed in the operation.
   */
  public int consumeByteString(byte[] target)
  {
    //Get the string length.
    int len = thisBuffer.getChar();

    //If we have insufficient space in the target, throw exception.
    if(target.length < len)
    {
      //Move to end.
      thisBuffer.position(thisBuffer.position() + len);
      throw new IllegalArgumentException();
    }
    
    //Copy the data into the array.
    thisBuffer.get(target);
    
    //Return the array.
    return len;
  }

  /**
   * <p>Consume a byte string.  A new byte buffer will be created to return the data to the caller.
   * 
   * @return byte string that the data have been consumed to.  This byte string must have been produced with a corresponding {@link SequentialMessageStreamWriter#produceByteString(byte[])} instruction.
   */
  public byte[] consumeByteString()
  {
    //Get the string length.
    int len = thisBuffer.getChar();

    byte[] target = new byte[len];
    
    //Copy the data into the array.
    thisBuffer.get(target);
    
    //Return the newly-minted byte array.
    return target;
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
   * <p>Transfer all available <b>block</b> data to the given byte buffer.  Less than the whole block could be
   * transferred if a limit has been set.
   *  
   * @param dst the ByteBuffer destination for consumable bytes.
   */
  public void transferToBuffer(ByteBuffer dst)
  {
    //Transfer data from the reader buffer to the buffer specified.
    dst.put(thisBuffer);
  }
}
