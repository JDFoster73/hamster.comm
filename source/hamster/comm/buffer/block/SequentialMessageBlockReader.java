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
import hamster.comm.buffer.block.itf.ReadBlockHandler;
import hamster.comm.buffer.block.itf.WriteBlockHandler;

/**
 * A {@link SequentialMessageBlockReader} instance models the read phase of a ByteBuffer-style object.  This object is loaded with
 * stream data and can then consume that data in the order that it was produced in.  For that purpose, the object
 * provides <code>consumeXXX()</code> methods to consume primitive data types.
 * 
 * <p>The {@link WriteBlockHandler} interface is also implemented for convenience.  The reader instance can be used to write all available
 * block data to the given writer.
 * 
 * @author jdf19
 */
public class SequentialMessageBlockReader implements WriteBlockHandler
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
   * <p>The block start for the currently processed message block.
   */
  private int blockStart = -1;

  /**
   * <p>When true, the next operation called on this instance <b>ABSOLUTELY MUST</b> be {@link WriteBlockHandler#writeMessageBlock(SequentialMessageBlockWriter)}.
   * This is a work-around for the Hamster system philosophy requiring minimal state.  In order to be able to move a chunk of data to another buffer easily out
   * of the currently-defined message limits, we set a temporary limit on the internal ByteBuffer which allows the amount of data to copy to be specified.
   * <p>Immediately after the {@link WriteBlockHandler#writeMessageBlock(SequentialMessageBlockWriter)} operation is complete, the buffer limit will be restored
   * and consumption of individual data primitives can continue up to the message limit.  
   */
  private boolean nextOpWriteBlock = false;
  
  /**
   * <p>If the next op required is a write block operation, the original message limit will be stored here.
   */
  private int nextOpWriteBlockLimit = -1;
  
  /**
   * Create a <code>SequentialBufferReader</code> instance with the given underlying ByteBuffer.
   * 
   * @param incomingBuffer the underlying ByteBuffer object that the sequential buffer uses to store and access the stream data.
   */
  SequentialMessageBlockReader(ByteBuffer incomingBuffer)
  {
    //Assign the parameter to the internal buffer.
    thisBuffer = incomingBuffer;
  }
  
  /**
   * Create a <code>SequentialBufferReader</code> instance.  The underlying buffer will be set with a subsequent call to {@link SequentialMessageBlockReader#updateBuffer(ByteBuffer)}.
   */
  SequentialMessageBlockReader()
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
   * <p>Start processing a message block.  Save the start position for calculation purposes.  The owning
   * caller will already have set up the buffer position and limit prior to calling this method.
   */
  void startBlock(int blockStartPos)
  {
    this.blockStart = blockStartPos;
    
    //Next op block.
    nextOpWriteBlock = false;
    nextOpWriteBlockLimit = -1;
  }
  
  /**
   * <p>Set the block start position to -1 which indicates that there is no block set.
   */
  void end()
  {
    //Clear the block start ix.
    this.blockStart = -1;

    //Clear write block limits.
    if(nextOpWriteBlock)
    {
      //Restore buffer limit.
      thisBuffer.limit(nextOpWriteBlockLimit);
      //Reset state.
      nextOpWriteBlockLimit = -1;
      //Unset flag.
      nextOpWriteBlock = false;
    }
  }

  /**
   * <p>If we are doing a nested produce there's a chance a rebuffer will cause a pointer adjustment.
   * Cater for this by adjusting the block start pointer if it is defined.
   * 
   * @param blockStartPointerAdj the number of bytes to add to or subtract from the currently defined block start position. 
   */
  void adjustBlockStart(int blockStartPointerAdj)
  {
    if(blockStart != -1) blockStart += blockStartPointerAdj;
  }
  
  /**
   * <p>Return the block start pointer.  Useful for the caller to be able to store the block start for nested reads.
   * 
   * @return the block start position.
   */
  int blockStartPosition()
  {
    //Return the block start position.
    return blockStart;
  }
  
  /**
   * <p>Consume a <code>double</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>double</code> value consumed from the reader.
   */
  public double consumeDouble()
  {
    //Check valid.
    checkValid();
    
    return thisBuffer.getDouble();
  }
  
  /**
   * <p>Consume a <code>float</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>float</code> value consumed from the reader.
   */
  public float consumeFloat()
  {
    //Check valid.
    checkValid();
    
    return thisBuffer.getFloat();
  }
  
  /**
   * <p>Consume a <code>long</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>long</code> value consumed from the reader.
   */
  public long consumeLong()
  {
    //Check valid.
    checkValid();
    
    return thisBuffer.getLong();
  }

  /**
   * <p>Consume an <code>int</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>int</code> value consumed from the reader.
   */
  public int consumeInt()
  {
    //Check valid.
    checkValid();
    
    return thisBuffer.getInt();
  }

  /**
   * <p>Consume a <code>short</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>short</code> value consumed from the reader.
   */
  public short consumeShort()
  {
    //Check valid.
    checkValid();
    
    return thisBuffer.getShort();
  }
  
  /**
   * <p>Consume a <code>char</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>char</code> value consumed from the reader.
   */
  public char consumeChar()
  {
    //Check valid.
    checkValid();
    
    return thisBuffer.getChar();
  }

  /**
   * <p>Consume a <code>byte</code> datatype.  Enough data must be left in the block to complete this call or an exception will be thrown.
   * 
   * @return <code>byte</code> value consumed from the reader.
   */
  public byte consumeByte()
  {
    //Check valid.
    checkValid();
    
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
    //Check valid.
    checkValid();
    
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
    //Check valid.
    checkValid();
    
    //Consume the data.
    thisBuffer.get(byteArray);
  }

  /**
   * <p>Consume an array of bytes from the reader into a newly created byte array instance.
   * 
   */
  public byte[] consumeBytes()
  {
    //Check valid.
    checkValid();
    
    //Create byte array.
    byte[] byteArray = new byte[bytesRemainingInBlock()];
    
    //Consume the data.
    thisBuffer.get(byteArray);
    
    //Return the byte array.
    return byteArray;
  }

  /**
   * <p>Consume a wide (16-bit code point) character string from the reader.
   * 
   * @return the string value from the reader.  This must have been produced with a corresponding {@link SequentialMessageBlockWriter#produceCharacterWString(String)} instruction.
   */
  public String consumeCharWString(HeaderSpec headerSpec)
  {
    //Check valid.
    checkValid();
    
    //Make sure the string builder is clear.
    this.charStringBuilder.setLength(0);
    
    //Get the string length.
    int len = getHeaderLen(headerSpec.width);
    
    //Adjust the length if the field value specifies the whole byte string field INCLUDING the header bytes.  If that is the case
    //then subtract the header width from the length.
    if(headerSpec.includeHeaderInLength)
    {
      len -= headerSpec.width.getWidthBytes();
    }
    
    //Copy the chars into the given buffer.
    for(int i = 0; i < len; i++)
    {
      this.charStringBuilder.append(thisBuffer.getChar());
    }
    
    return this.charStringBuilder.toString();
  }
  
  /**
   * <p>Consume a narrow (8-bit code point) character string from the reader.
   * 
   * @return the string value from the reader.  This must have been produced with a corresponding {@link SequentialMessageBlockWriter#produceCharacterWString(String)} instruction.
   */
  public String consumeCharString(HeaderSpec headerSpec)
  {
    //Get the string bytes.
    byte[] bytesInString = consumeByteString(headerSpec);
    
    //Construct string and return.
    return new String(bytesInString);
    
//    //Check valid.
//    checkValid();
//    
//    //Get the string length.
//    int len = getHeaderLen(headerWidth);
//    
//    //Create bytes to construct the string with.
//    byte[] bTemp = new byte[len];
//    thisBuffer.get(bTemp);
//    
//    return new String(bTemp);
  }

  /**
   * <p>Consume a byte string from the reader to the target.
   * 
   * @param target the byte string to consume the byte data to.  This must have been produced with a corresponding {@link SequentialMessageBlockWriter#produceByteString(byte[])} instruction.
   * @return the number of bytes consumed in the operation.
   */
  public int consumeByteString(HeaderSpec headerSpec, byte[] target)
  {
    //Check valid.
    checkValid();
    
    //Get the string length.
    int len = getHeaderLen(headerSpec.width);

    //Adjust the length if the field value specifies the whole byte string field INCLUDING the header bytes.  If that is the case
    //then subtract the header width from the length.
    if(headerSpec.includeHeaderInLength)
    {
      len -= headerSpec.width.getWidthBytes();
    }
    
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
   * @return byte string that the data have been consumed to.  This byte string must have been produced with a corresponding {@link SequentialMessageBlockWriter#produceByteString(byte[])} instruction.
   */
  public byte[] consumeByteString(HeaderSpec headerSpec)
  {
    //Check valid.
    checkValid();
    
    //Get the string length.
    int len = getHeaderLen(headerSpec.width);

    //Adjust the length if the field value specifies the whole byte string field INCLUDING the header bytes.  If that is the case
    //then subtract the header width from the length.
    if(headerSpec.includeHeaderInLength)
    {
      len -= headerSpec.width.getWidthBytes();
    }

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
    //Check valid.
    checkValid();

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
    //Check valid.
    checkValid();

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
    //Check valid.
    checkValid();

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
    //Check valid.
    checkValid();

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
    //Check valid.
    checkValid();

    //Replace the byte with the new one.
    thisBuffer.putLong(by);
  }

  /**
   * <p>Process a sub block, setting constraints within this message.
   * 
   * @param handler the block handler which will consume up to the rest of the data in the currently defined block.
   * @return true if data were consumed by the handler.
   */
  public boolean processReadBlockHandler(ReadBlockHandler handler)
  {
    //Check valid.
    checkValid();

    //Store buffer start position.
    int bufferStartPos = blockStart;
    
    //Set the embedded buffer start block position.
    blockStart = thisBuffer.position();
    
    try
    {
      //Handle completion.
      return handler.readMessageBlock(this);
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
      //Restore the block start position.
      this.blockStart = bufferStartPos;
    }
    
    //
  }

  /**
   * <p>Process a sub block, setting constraints within this message.
   * 
   * @param handler the block handler which will consume up to the rest of the data in the currently defined block.
   */
  public void processReadAllHandler(ReadAllHandler handler)
  {
    //Check valid.
    checkValid();

    //Store buffer start position.
    int bufferStartPos = blockStart;
    
    //Set the embedded buffer start block position.
    blockStart = thisBuffer.position();
    
    try
    {
      //Handle completion.
      handler.readMessageAll(this);
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
      //Restore the block start position.
      this.blockStart = bufferStartPos;
    }
    
    //
  }


  /**
   * <p>Instead of consuming, skip the given number of bytes.  Useful for skipping unused data from protocol headers etc.
   * 
   * @param bytes the number of bytes to skip.
   */
  public void skip(int bytes)
  {
    //Check valid.
    checkValid();
    
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
    //Check valid.
    checkValid();
    
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
   * This gives a fixed value of the total number of bytes in the block being processed.
   *  
   *  @return the total number of bytes that have been available to read
   */
  public int bytesInBlock()
  {
    //Check valid.
    checkValid();
    
    // The byte buffer has been set up with the position at the start of the message block and the limit at the
    // end of the message block.  Use these values to compute the return value.
    return thisBuffer.limit() - this.blockStart;
  }

  /**
   * The current block position, i.e. the number of bytes that have been read from the currently processed
   * block so far.
   * 
   * @return the number of bytes that have been read so far.
   */
  public int currentBlockReadPosition()
  {
    //Check valid.
    checkValid();
    
    // The byte buffer has been set up with the position at the start of the message block and the limit at the
    // end of the message block.  Use these values to compute the return value.
    return thisBuffer.position() - this.blockStart;
  }

  /**
   * <p>Check that a block has been set.  If no block is set then the reference has been stored and is being used
   * outside of a block call.  The point of reading all message data in one call is to remove the complexity of 
   * dealing with a buffer object that can be filled and drained simultaneously.
   * <p>The next operation will not be valid if the {@link #nextOpWriteBlock} flag is set.
   */
  private void checkValid()
  {
    if((blockStart == -1) || nextOpWriteBlock) throw new IllegalStateException();
  }

  /**
   * <p>Let the owner query the writer for validititidy.
   * 
   * @return
   */
  boolean isValid()
  {
    return (blockStart >= 0);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //BLOCK POINTER MANIPULATION METHODS.
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * Rewind the read position to the start of the current message block.
   */
  public void rewindToBlockStartPosition()
  {
    //Check there's a valid block position set.
    checkValid();
    
    //Navigate to the requested block position.
    thisBuffer.position(blockStart);
    
    //Restore the message limit if write block is set.
    if(nextOpWriteBlock)
    {
      //Restore limit.
      thisBuffer.limit(nextOpWriteBlockLimit);
      
      //Reset flag.
      nextOpWriteBlock = false;
      nextOpWriteBlockLimit = -1;
    }
  }

  /**
   * <p>Rewind the current byts pointer by the given number of bytes in the message block.  Can not rewind to before the beginning of the currently
   * defined message block.
   * 
   * @param bytesFromCurrentToRewind the number of bytes to rewind - must not be larger than the number of bytes read so far given by {@link #currentBlockReadPosition()}.
   */
  public void rewind(int bytesFromCurrentToRewind)
  {
    //Check valid.
    checkValid();

    //Calculate the rollback position.
    int rollbackPosition = thisBuffer.position() - bytesFromCurrentToRewind;
    
    //Check we are advanced sufficiently from the start of the message block to roll back.
    if(rollbackPosition < blockStart) throw new IllegalArgumentException();
    
    //Roll back the buffer position.
    thisBuffer.position(rollbackPosition);
  }

  /**
   * <p>Set the current byte read position for the currently processed block.
   *  
   * @param bytesFromStartToSetPosition the consume position to set.
   * @return this {@link SequentialMessageBlockReader} instance for telescoping calls.
   */
  public SequentialMessageBlockReader setBlockPosition(int bytesFromStartToSetPosition)
  {
    //Check valid.
    checkValid();

    //Calculate the rollback position.
    int bytePosition = blockStart + bytesFromStartToSetPosition;
    
    //Check we can set that position within the limits of the defined block.
    if( (bytePosition < blockStart) || (bytePosition >= thisBuffer.limit()) ) throw new IllegalArgumentException();
    
    //Roll back the buffer position.
    thisBuffer.position(bytePosition);
    
    //Return this.
    return this;
  }
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * <p>Drain consumable data from this block into the given writer.
   * 
   * @param writer the target of data that is consumed (drained) from this reader.
   */
  public void writeMessageBlock(SequentialMessageBlockWriter writer)
  {
    //Check there's a valid block position.
    if((blockStart == -1)) throw new IllegalStateException();

    try
    {
      //Transfer all available message data in the reader block to the writer.
      writer.transferByteBufferContent(thisBuffer);
    }
    finally
    {
      //If sub-block set, restore the buffer limit.
      if(this.nextOpWriteBlock)
      {
        //Restore limit.
        thisBuffer.limit(nextOpWriteBlockLimit);
        
        //Unset the next op flag.
        this.nextOpWriteBlock = false;
        this.nextOpWriteBlockLimit = -1;
      }
    }
  }

  /**
   * <p>When it is known that the next call after this one on this instance will be {@link WriteBlockHandler#writeMessageBlock(SequentialMessageBlockWriter)},
   * it may be that a limit is required so that the operation only moves a required fixed block length rather than the whole remaining block.
   * <p>The next call <b>ABSOLUTELY MUST BE</b> {@link WriteBlockHandler#writeMessageBlock(SequentialMessageBlockWriter)} or an exception will be 
   * thrown.
   * <p>Once the operation is complete, the limit will be removed and the remaining data can be consumed as normal.
   * 
   * @param blockSizeBytes set a byte limit on the current block.
   * @return this {@link SequentialMessageBlockReader} instance for telescoping calls.
   */
  public SequentialMessageBlockReader setWriteBlockLimit(int blockSizeBytes)
  {
    //Check we have enough bytes.
    if(blockSizeBytes > bytesRemainingInBlock()) throw new IllegalArgumentException();
    
    //Save the limit.
    this.nextOpWriteBlockLimit = thisBuffer.limit();
    
    //Set the buffer limit to the required sub-block length.
    thisBuffer.limit(thisBuffer.position() + blockSizeBytes);
    
    //Set the next op is write block flag.
    this.nextOpWriteBlock = true;
    
    //Return this instance.
    return this;
  }

  /**
   * <p>Transfer all available <b>block</b> data to the given byte buffer.  Less than the whole block could be
   * transferred if a limit has been set.
   *  
   * @param dst the ByteBuffer destination for consumable bytes.
   */
  public void transferToBuffer(ByteBuffer dst)
  {
    checkValid();
    
    //Transfer data from the reader buffer to the buffer specified.
    dst.put(thisBuffer);
  }
  
  /**
   * <p>Get the char or byte string length header from the stream and return the value.
   * 
   * @param headerWidth
   * @return
   */
  private int getHeaderLen(HEADER_WIDTH headerWidth)
  {
    int len;
    switch(headerWidth)
    {
      case U1:
        len = thisBuffer.get() & 0xff;
        break;
      case U2:
        len = thisBuffer.getChar();        
        break;
      case U4:
        len = thisBuffer.getInt() & 0xffff_ffff;
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    return len;
  }
}
