/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.buffer.block;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import hamster.comm.buffer.block.HeaderSpec.HEADER_WIDTH;
import hamster.comm.buffer.block.itf.ReadAllHandler;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
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
 * for sending or receiving data.  There are no conflicts arising from the timings of sending and receiving
 * because those operations use entirely different buffer space.
 * 
 * @author jdf19
 */
public final class SequentialMessageBlockWriter implements ReadAllHandler
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
   * <p>Block start pos.  If < 0, the writer is not initialised and must throw an exception if any of the methods are accessed.
   * If >= 0, it is the buffer start position of the message block.  Post processing navigation <b>MUST NOT</b> travel beyond this.
   */
  private int blockStartPos = -1;
  
  /**
   * <p>Update listener upon rebuffer operation.
   */
  private final RebufferingListener rebufferListener;

  /**
   * Construct the sequential buffer writer with the given underlying byte buffer and listener.
   * @param bufferFact 
   * 
   * @param bb the initial underlying byte buffer to use.
   * @param listener write listener.  This can be <code>null</code> if no write listener functionality is required.
   */
  SequentialMessageBlockWriter(GrowUpdateExpandableBufferFactory bufferFact, RebufferingListener rebufferListener)
  {
    //Store rebuffering factory reference.
    factory = bufferFact;
    
    //Store rebuffering listener reference.
    this.rebufferListener = rebufferListener;

    //Create initial buffer and call rebuffer listener.
    this.thisBuffer = factory.initialiseBuffer();
    rebufferListener.handleRebuffer(thisBuffer);
  }
  
  /**
   * <p>Start writing a block message at the current write position.  Return this position to the caller.
   * 
   * @return the block start byte position in the buffer.
   */
  int start()
  {
    blockStartPos = thisBuffer.position();
    return blockStartPos;
  }
  
  /**
   * <p>End writing a block message.  
   */
  void end()
  {
    blockStartPos = -1;
  }

  
  /**
   * <p>Advance the write position by the given relative position.  This position can be a negative number, as long
   * as it is within the bounds of the existing write block.
   * 
   * @param bytesToAdvance the number of bytes to advance the write position - can be negative.
   * @return this writer instance.
   */
  public SequentialMessageBlockWriter advance(int bytesToAdvance)
  {
    //Check valid.
    checkValid();
    
    reserveSpaceForProduceOperation(bytesToAdvance);
    thisBuffer.position(thisBuffer.position() + bytesToAdvance);

    return this;
  }

  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialMessageBlockWriter produceFloat(float data)
  {
    //Check valid.
    checkValid();
    
    reserveSpaceForProduceOperation(Float.BYTES);
    thisBuffer.putFloat(data);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialMessageBlockWriter produceDouble(double data)
  {
    //Check valid.
    checkValid();
    
    reserveSpaceForProduceOperation(Double.BYTES);
    thisBuffer.putDouble(data);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialMessageBlockWriter produceLong(long data)
  {
    //Check valid.
    checkValid();
    
    reserveSpaceForProduceOperation(Long.BYTES);
    thisBuffer.putLong(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialMessageBlockWriter produceInt(int data)
  {
    //Check valid.
    checkValid();
    
    reserveSpaceForProduceOperation(Integer.BYTES);
    thisBuffer.putInt(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialMessageBlockWriter produceShort(short data)
  {
    //Check valid.
    checkValid();
    
    reserveSpaceForProduceOperation(Short.BYTES);
    thisBuffer.putShort(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialMessageBlockWriter produceChar(char data)
  {
    //Check valid.
    checkValid();
    
    reserveSpaceForProduceOperation(Character.BYTES);
    thisBuffer.putChar(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialMessageBlockWriter produceByte(byte data)
  {
    //Check valid.
    checkValid();
    
    reserveSpaceForProduceOperation(Byte.BYTES);
    thisBuffer.put(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialMessageBlockWriter produceBytes(byte[] data)
  {
    //Check valid.
    checkValid();
    
    reserveSpaceForProduceOperation(data.length);
    thisBuffer.put(data);
    return this;
  }
  
  /**
   * {@inheritDoc}
   */
  //@Override
  public SequentialMessageBlockWriter produceBytes(byte[] data, int start, int len)
  {
    //Check valid.
    checkValid();
    
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
  public SequentialMessageBlockWriter produceCharacterWString(HeaderSpec headerSpec, String string)
  {
    //Check valid.
    checkValid();
    
    //Check there is enough space for:
    // - the 2-byte length prefix
    // - each character in the string.
    reserveSpaceForProduceOperation(headerSpec.width.getWidthBytes() + (string.length() * Character.BYTES));
    
    // Number of code points in the character string.
    int strLen = string.length() + (headerSpec.includeHeaderInLength ? headerSpec.width.getWidthBytes() : 0);
    putHeaderLen(headerSpec.width, strLen);
    
    // Copy the characters.
    for (int i = 0; i < string.length(); i++)
    {
      thisBuffer.putChar(string.charAt(i));
    }
    return this;
  }
  
  /**
   * Produce the given character string into this buffer writer.  The string must have
   * less than 65535 bytes.
   * <p>
   * The characters will be copied into the buffer from the string parameter but will first
   * be prefixed by a 2-byte unsigned length value which specifies the number of 
   * bytes in the string.
   * 
   * @param string the string to produce to the writer.
   * @return this buffer writer instance reference.
   */
  public SequentialMessageBlockWriter produceCharacterString(HeaderSpec headerSpec, String string)
  {
    return produceByteString(headerSpec, string.getBytes());
    //    //Check valid.
//    checkValid();
//    
//    //Get the string bytes.
//    byte[] stringBytes = string.getBytes();
//    
//    //Check there is enough space for:
//    // - the 2-byte length prefix
//    // - each character in the string.
//    reserveSpaceForProduceOperation(headerSpec.width.getWidthBytes() + stringBytes.length);
//    
//    int strLen = stringBytes.length + (headerSpec.includeHeaderInLength ? headerSpec.width.getWidthBytes() : 0);
//
//    // Number of code points in the character string.
//    putHeaderLen(headerSpec.width, strLen);
//    
//    // Copy the characters.
//    thisBuffer.put(stringBytes);
//    
//    return this;
  }

  /**
   * <p>Produce an array of bytes with a 2-byte unsigned integer prefix specifying
   * the number of bytes produced.
   * 
   * @param bytes the byte array to produce data from.
   * @return this buffer writer instance reference.
   */
  public SequentialMessageBlockWriter produceByteString(HeaderSpec headerSpec, byte[] bytes)
  {
    //Check valid.
    checkValid();
    
    reserveSpaceForProduceOperation(headerSpec.width.getWidthBytes() + bytes.length);
    
    //Calculate header length value - inclusive or exclusive of itself.
    int strLen = bytes.length + (headerSpec.includeHeaderInLength ? headerSpec.width.getWidthBytes() : 0);

    // Number of code points in the character string.
    putHeaderLen(headerSpec.width, strLen);
    
    // Put the byte array.
    thisBuffer.put(bytes);
    
    return this;
  }

  /**
   * <p>Produce a byte at the given message block position.
   * 
   * @param b the byte value to produce to the writer.
   * @param pos the buffer byte position to produce the byte value to.
   * @return this buffer writer instance reference.
   */
  public SequentialMessageBlockWriter produceByteAt(byte b, int pos)
  {
    //Check valid.
    checkValid();
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
  public SequentialMessageBlockWriter produceShortAt(short b, int pos)
  {
    //Check valid.
    checkValid();
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
  public SequentialMessageBlockWriter produceCharAt(char value, int pos)
  {
    //Check valid.
    checkValid();
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
  public SequentialMessageBlockWriter produceIntAt(int value, int pos)
  {
    //Check valid.
    checkValid();
    thisBuffer.putInt(this.blockStartPos + pos, value);
    return this;
  }

  /**
   * <p>Add a writable block from the given writable block handler.
   * 
   * @param handler the handler to take a block of data from which is to be produced to this writer.
   */
  public void addWritableBlock(WriteBlockHandler handler)
  {
    //Check valid.
    checkValid();

    //Store buffer start position.
    int bufferStartPos = thisBuffer.position();
    
    //Store block start pos.
    int savedBlockStart = this.blockStartPos;
    
    //Set the block start to the embedded message start position.
    this.blockStartPos = bufferStartPos;
    
    try
    {
      //Write the message block data.
      handler.writeMessageBlock(this);
    }
    catch(Throwable t)
    {
      //Roll back to start pos.
      thisBuffer.position(bufferStartPos);
      
      //Rethrow
      throw t;
    }
    finally
    {
      //Set limit to capacity.
      //TODO WHY??
      thisBuffer.limit(thisBuffer.capacity());
      
      //Restore the buffer start position.
      this.blockStartPos = savedBlockStart;
    }
    
    //
  }

  /**
   * <p>Take the available data in the source buffer argument and transfer it to this writer's buffer.
   * 
   * @param sourceBuffer the ByteBuffer (must be flipped and ready to transfer the data) from which to transfer data from.  The data will be produced to this writer.
   */
  public int transferByteBufferContent(ByteBuffer sourceBuffer)
  {
    //Check valid.
    checkValid();

    //Make space available in the destination.
    reserveSpaceForProduceOperation(sourceBuffer.remaining());
    
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
    rebufferListener.handleRebuffer(thisBuffer);
  }

  /**
   * <p>Check that a block has been set.  If no block is set then the reference has been stored and is being used
   * outside of a block call.  The point of reading all message data in one call is to remove the complexity of 
   * dealing with a buffer object that can be filled and drained simultaneously.
   */
  private void checkValid()
  {
    if(blockStartPos == -1) throw new IllegalStateException();
  }

  /**
   * <p>Let the owner query the writer for validititidy.
   * 
   * @return
   */
  boolean isValid()
  {
    return (blockStartPos >= 0);
  }
  
  /**
   * <p>Accept block data from a message block reader.  It is done this way round because the reader may contain
   * more data in its defined block than the writer can accept without rebuffering.  Therefore the writer should
   * be allowed to determine its space requirements before the transfer.
   *  
   * @param sequentialMessageBlockReader the reader to transfer a block of data from.  MUST NOT have the same underlying buffer storage i.e. be from the same pipeline buffer instance.
   */
  void acceptReaderBlockData(SequentialMessageBlockReader sequentialMessageBlockReader)
  {
    //Can we accept the data?
    reserveSpaceForProduceOperation(sequentialMessageBlockReader.bytesInBlock());
    
    //Transfer.
    thisBuffer.put(sequentialMessageBlockReader.thisBuffer);
  }

  /**
   * <p>Return the number of bytes written in the current block.
   * 
   * @return the number of bytes in the current write block.
   */
  public int blockMessageBytes()
  {
    return thisBuffer.position() - blockStartPos;
  }

  /**
   * <p>Roll back to the message start.  This is useful if there has been an exception while building the message
   * and different contents are required.  For example, a message could start with a general status code.  If a problem
   * occurs while building the message contents then it can be rolled back and an error status code produced instead,
   * along with any useful diagnostic information.
   */
  public void rollbackToStart()
  {
    //Check valid.
    checkValid();
    
    //Roll the buffer back.
    thisBuffer.position(blockStartPos);
  }

  @Override
  public void readMessageAll(SequentialMessageBlockReader reader)
  {
    reader.writeMessageBlock(this);
  }
  
  /**
   * <p>Put the char or byte string length header on the stream.
   * 
   * @param headerWidth
   * @return
   */
  private int putHeaderLen(HEADER_WIDTH headerWidth, int len)
  {
    switch(headerWidth)
    {
      case U1:
        thisBuffer.put((byte) (len & 0xff));
        break;
      case U2:
        thisBuffer.putChar((char) (len & 0xffff));
        break;
      case U4:
        thisBuffer.putInt((len & 0xffff_ffff));
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    return len;
  }

}
