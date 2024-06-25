/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.buffer.stream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import hamster.comm.buffer.factory.GrowUpdateExpandableBufferFactory;

/**
 * <p>This sequential buffer writer allows buffer data to be produced.  Underlying storage is implemented
 * in a {@link java.nio.ByteBuffer} instance.
 * <p>This scheme attempts to simplify the use of the standard Java {@link java.nio.ByteBuffer}.  The typical
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
 * for sending <b>or</b> receiving data.  There are no conflicts arising from the timings of sending and receiving
 * because those operations use entirely different buffer space.
 * 
 * @author jdf19
 */
public abstract class SequentialMessageStreamWriter
{  
  /**
   * The underlying byte buffer used for storing and processing the write data.
   */
  protected ByteBuffer thisBuffer;
    
  /**
   * Allow rebuffering if we run out of space.
   */
  private final GrowUpdateExpandableBufferFactory factory;

  /**
   * Construct the sequential buffer writer with the given underlying byte buffer and listener.
   * @param bufferFact 
   * 
   * @param bb the initial underlying byte buffer to use.
   * @param listener write listener.  This can be <code>null</code> if no write listener functionality is required.
   */
  public SequentialMessageStreamWriter(GrowUpdateExpandableBufferFactory bufferFact)
  {
    //Store rebuffering factory reference.
    factory = bufferFact;
    
    //Create initial buffer and call rebuffer listener.
    this.thisBuffer = factory.initialiseBuffer();
  }
  
  /**
   * <p>Produce a float value.
   * 
   * @param data value to produce.
   * @return reference to <code>this</code>.
   */
  public SequentialMessageStreamWriter produceFloat(float data)
  {
    reserveSpaceForProduceOperation(Float.BYTES);
    thisBuffer.putFloat(data);
    return this;
  }

  /**
   * <p>Produce a double value.
   * 
   * @param data value to produce.
   * @return reference to <code>this</code>.
   */
  public SequentialMessageStreamWriter produceDouble(double data)
  {
    reserveSpaceForProduceOperation(Double.BYTES);
    thisBuffer.putDouble(data);
    return this;
  }

  /**
   * <p>Produce a long value.
   * 
   * @param data value to produce.
   * @return reference to <code>this</code>.
   */
  public SequentialMessageStreamWriter produceLong(long data)
  {
    reserveSpaceForProduceOperation(Long.BYTES);
    thisBuffer.putLong(data);
    return this;
  }
  
  /**
   * <p>Produce a int value.
   * 
   * @param data value to produce.
   * @return reference to <code>this</code>.
   */
  public SequentialMessageStreamWriter produceInt(int data)
  {
    reserveSpaceForProduceOperation(Integer.BYTES);
    thisBuffer.putInt(data);
    return this;
  }
  
  /**
   * <p>Produce a short value.
   * 
   * @param data value to produce.
   * @return reference to <code>this</code>.
   */
  public SequentialMessageStreamWriter produceShort(short data)
  {
    reserveSpaceForProduceOperation(Short.BYTES);
    thisBuffer.putShort(data);
    return this;
  }
  
  /**
   * <p>Produce a char value.
   * 
   * @param data value to produce.
   * @return reference to <code>this</code>.
   */
  public SequentialMessageStreamWriter produceChar(char data)
  {
    reserveSpaceForProduceOperation(Character.BYTES);
    thisBuffer.putChar(data);
    return this;
  }
  
  /**
   * <p>Produce a byte value.
   * 
   * @param data value to produce.
   * @return reference to <code>this</code>.
   */
  public SequentialMessageStreamWriter produceByte(byte data)
  {
    reserveSpaceForProduceOperation(Byte.BYTES);
    thisBuffer.put(data);
    return this;
  }
  
  /**
   * <p>Produce an array of bytes.
   * 
   * @param data value to produce.
   * @return reference to <code>this</code>.
   */
  public SequentialMessageStreamWriter produceBytes(byte[] data)
  {
    reserveSpaceForProduceOperation(data.length);
    thisBuffer.put(data);
    return this;
  }
  
  /**
   * <p>Produce an array of bytes.
   * 
   * @param data value to produce.
   * @return reference to <code>this</code>.
   */
  public SequentialMessageStreamWriter produceBytes(byte[] data, int start, int len)
  {
    reserveSpaceForProduceOperation(len);
    thisBuffer.put(data, start, len);
    return this;
  }
  
  /**
   * Produce the given character string into this buffer writer.  The string must have
   * less than 65535 unicode code units.
   * <p>
   * The characters will be copied into the buffer from the string parameter but will first
   * be prefixed by a 2-byte unsigned length value which specifies the number of 
   * unicode code points in the string.
   * 
   * @param string the string to produce to the writer.
   * @return this buffer writer instance reference.
   */
  public SequentialMessageStreamWriter produceCharacterString(String string)
  {
    //Check there is enough space for:
    // - the 2-byte length prefix
    // - each character in the string.
    reserveSpaceForProduceOperation(Character.BYTES + (string.length() * Character.BYTES));
    
    // Number of code points in the character string.
    thisBuffer.putChar((char) string.length());
    
    // Copy the characters.
    for (int i = 0; i < string.length(); i++)
    {
      thisBuffer.putChar(string.charAt(i));
    }
    return this;
  }
  
  /**
   * <p>Produce an array of bytes with a 2-byte unsigned integer prefix specifying
   * the number of bytes produced.
   * 
   * @param bytes the byte array to produce data from.
   * @return this buffer writer instance reference.
   */
  public SequentialMessageStreamWriter produceByteString(byte[] bytes)
  {    
    reserveSpaceForProduceOperation(Character.BYTES + bytes.length);
    
    // Number of code points in the character string.
    thisBuffer.putChar((char) bytes.length);
    
    // Put the byte array.
    thisBuffer.put(bytes);
    
    return this;
  }

  /**
   * <p>Produce a byte at the given buffer position.
   * 
   * @param b the byte value to produce to the writer.
   * @param pos the buffer byte position to produce the byte value to.
   * @return this buffer writer instance reference.
   */
  public SequentialMessageStreamWriter produceByteAt(byte b, int pos)
  {
    thisBuffer.put(pos, b);
    return this;
  }
  
  /**
   * <p>Produce a short at the given buffer position.
   * 
   * @param b the short value to produce to the writer.
   * @param pos the buffer byte position to produce the short value to.
   * @return  this buffer writer instance reference.
   */
  public SequentialMessageStreamWriter produceShortAt(short b, int pos)
  {
    thisBuffer.putShort(pos, b);
    return this;
  }

  /**
   * <p>Produce a char at the given buffer position.
   * 
   * @param value the char value to produce to the writer.
   * @param pos the buffer byte position to produce the char value to.
   * @return this buffer writer instance reference.
   */
  public SequentialMessageStreamWriter produceCharAt(char value, int pos)
  {
    thisBuffer.putChar(pos, value);
    return this;
  }

  /**
   * <p>Produce a int at the given buffer position.
   * 
   * @param value the int value to produce to the writer.
   * @param pos the buffer byte position to produce the int value to.
   * @return this buffer writer instance reference.
   */
  public SequentialMessageStreamWriter produceIntAt(int value, int pos)
  {
    thisBuffer.putInt(pos, value);
    return this;
  }

  /**
   * <p>Take the available data in the source buffer argument and transfer it to this writer's buffer.
   * 
   * @param sourceBuffer the ByteBuffer (must be flipped and ready to transfer the data) from which to transfer data from.  The data will be produced to this writer.
   */
  public void transferByteBufferContent(ByteBuffer sourceBuffer)
  {
    //Make space available in the destination.
    reserveSpaceForProduceOperation(sourceBuffer.remaining());
    
    //Put the data from the source buffer.
    thisBuffer.put(sourceBuffer);
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
  
  /**
   * Called internally when a produceXXX(...) method is called in order to check that there
   * is space to write the requested data into the buffer without throwing an overflow exception.
   * <p>
   * If there are not enough bytes to write the requested data into the buffer then the abstract
   * <code>rebuffer()</code> method is called.  The concrete implementation is responsible for
   * rebuffering without disturbing its internal state.
   * 
   * @param bytesToWrite the number of bytes that are to be written to the buffer.
   */
  private void reserveSpaceForProduceOperation(int bytesToWrite)
  {
    if( (thisBuffer.position() + bytesToWrite) > thisBuffer.capacity())
    {
      //Rebuffer.
      rebuffer(thisBuffer.position() + bytesToWrite);
    }
  }
  
  /**
   * There is no space to write the given data point into the writer.  Create a new expanded underlying byte buffer and copy the buffer contents.
   */
  void rebuffer(int minimum)
  {
    thisBuffer = factory.replaceBuffer(thisBuffer, minimum);
    
    //Update listener.
    handleRebuffer(thisBuffer);
  }

  /**
   * <p>Call subclasses to handle a rebuffering event as a result of expanding the buffer.
   * 
   * @param buffer the newly-rebuffered buffer.
   */
  protected abstract void handleRebuffer(ByteBuffer buffer);
  
}
