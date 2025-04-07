package hamster.comm.buffer;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * <p>The general pipeline buffer simply buffers data bytes.
 *
 * @author jdf19
 */
public class PipelineBuffer extends BaseBuffer implements FillableChannelBuffer, DrainableChannelBuffer
{
  //If the produce flag is true then the buffer is to be filled.
  //If the flag is false, the buffer is to be consumed.
  //As the buffer is empty initiall and there is nothing to consume, start off in produce mode.
  private boolean produce = true;

  private int consumeIndex = 0;

  private int produceIndex = 0;

  //Set this to the internal buffer position at the end of the currently processed read message.
  private int readMessageEndPosition = 0;

  //Set this to the internal buffer position at start end of the currently processed read message.
  private int readMessageStartPosition = 0;

  //Set this to the internal buffer position at the start of the currently processed write message.
  private int writeMessageStartPosition = 0;

  //This is true if the owner has set a message block.  This operation must be completed before the buffer can be used
  //in any other way, or a runtime exception will be thrown.
//  private boolean operationLock = false;
//  private boolean messageActive = false;

  //This is true if there is currently an operation lock for a read block operation.
  private boolean readBlockActive = false;

  //This is true if there is currently an operation lock for a write block operation.
  private boolean writeBlockActive = false;

  //Only one operation can take place at once.  Re-entrant calls are not permitted.
  //Otherwise the complexity gets very difficult to manage and this library wants to
  //keep things simple.

  /**
   * Create an instance of the general pipeline buffer with the given factory and logger instances.
   *
   * @param bufferFact the buffer factory to use when creating an instance of the general pipeline buffer.
   */
  public PipelineBuffer(BufferFactory bufferFact)
  {
    //internalBuffer = bufferFact.newBuffer();
    super(bufferFact);
  }

  public PipelineBuffer produceFromBytes(byte[] bytesToTransferToThisBuffer)
  {
    try
    {
      //Check we can produce data - no read message is in construction.
      //checkProducable();

      //Set to produce.
      setProduceMode();

      //Add the bytes to the internal buffer.
      internalBuffer.put(bytesToTransferToThisBuffer);

      //Update the produce index.
      produceIndex = internalBuffer.position();

      //Return this.
      return this;
    }
    finally
    {
      //Release lock.
      //releaseOperationLock();
    }
  }

  /**
   * Transfer the raw byte data from the source array into this buffer instance.  There must be enough space in the
   * internal buffer to be able to transfer all bytes from the source array.
   *
   * @param bytesToTransferToThisBuffer
   * @param startIx
   * @param length
   * @return
   */
  public PipelineBuffer produceFromBytes(byte[] bytesToTransferToThisBuffer, int startIx, int length)
  {
    try
    {
      //Check we can produce data - no read message is in construction.
      //checkProducable();

      //Set to produce.
      setProduceMode();

      //Add the bytes to the internal buffer.
      internalBuffer.put(bytesToTransferToThisBuffer, startIx, length);

      //Update the produce index.
      produceIndex = internalBuffer.position();

      //Return this.
      return this;
    }
    finally
    {
      //Release lock.
      //releaseOperationLock();
    }
  }

  public PipelineBuffer produceFromByteBuffer(ByteBuffer producer)
  {
    try
    {
      //Check we can produce data - no read message is in construction.
      //checkProducable();

      //Set to produce.
      setProduceMode();

      //Produce to the given reference.
      internalBuffer.put(producer);

      //Update the produce index.
      produceIndex = internalBuffer.position();

      //Return this.
      return this;
    }
    finally
    {
      //Release lock.
      //releaseOperationLock();
    }
  }

  public PipelineBuffer produceDataFromPipelineBuffer(PipelineBuffer producer)
  {
    try
    {
      //Check we can produce data - no read message is in construction.
      //checkProducable();

      //Set to consume mode.
      setProduceMode();

      //Produce the data to this buffer instance.
      producer.transferToTargetBuffer(this);

      //Update the produce index.
      produceIndex = internalBuffer.position();

      //Return this.
      return this;
    }
    finally
    {
      //Release lock.
      //releaseOperationLock();
    }

  }

  private void transferToTargetBuffer(PipelineBuffer target)
  {
    try
    {
      //Check we can consume data - no write message is in construction.
      //checkConsumable();

      //Target is already in consume mode.  Put this buffer into produce mode.
      setConsumeMode();

      //Transfer as much data as possible from this buffer to the target buffer.
      target.internalBuffer.put(internalBuffer);

      //Update the consume index.
      consumeIndex = internalBuffer.position();
    }
    finally
    {
      //Release lock.
      //releaseOperationLock();
    }
  }

  void doTransferTo(ByteBuffer targetBuffer)
  {
    try
    {
      //Check we can consume data - no write message is in construction.
      //checkConsumable();

      //Target is already in consume mode.  Put this buffer into produce mode.
      setConsumeMode();

      //Transfer as much data as possible from this buffer to the target buffer.
      doBufferTransfer(internalBuffer, targetBuffer);

      //Update the consume index.
      consumeIndex = internalBuffer.position();
    }
    finally
    {
      //Release lock.
      //();
    }
  }

  void doTransferFrom(ByteBuffer sourceBuffer)
  {
    try
    {
      //Check we can produce data - no read message is in construction.
      //checkProducable();

      //Target is already in consume mode.  Put this buffer into produce mode.
      setProduceMode();

      //Transfer as much data as possible from this buffer to the target buffer.
      doBufferTransfer(sourceBuffer, internalBuffer);

      //Update the consume index.
      produceIndex = internalBuffer.position();
    }
    finally
    {
      //Release lock.
      //releaseOperationLock();
    }
  }
  /**
   * Base implementation.  Simply transfer bytes from source to target as is.
   *
   * @param source
   * @param target
   */
  protected void doBufferTransfer(ByteBuffer source, ByteBuffer target)
  {
    target.put(source);
  }

  public PipelineBuffer consumeBytes(byte[] bytesToTransferToThisBuffer, int startIx, int length)
  {
    try
    {
      //Check we can consume data - no write message is in construction.
      //checkConsumable();

      //Set to consume.
      setConsumeMode();

      //Consume the requested data to the given byte buffer.
      internalBuffer.get(bytesToTransferToThisBuffer, startIx, length);

      //Update the produce index.
      consumeIndex += length;

      //Return this.
      return this;
    }
    finally
    {
      //Release lock.
      //releaseOperationLock();
    }
  }

  public PipelineBuffer consumeBytes(byte[] bytesToTransferToThisBuffer)
  {
    try
    {
      //Check we can consume data - no write message is in construction.
      //checkConsumable();

      //Set to consume.
      setConsumeMode();

      //Consume the requested data to the given byte buffer.
      internalBuffer.get(bytesToTransferToThisBuffer);

      //Update the produce index.
      consumeIndex += bytesToTransferToThisBuffer.length;

      //Return this.
      return this;
    }
    finally
    {
      //Release lock.
      //releaseOperationLock();
    }
  }

  protected void setProduceMode()
  {
    //We can produce data if there is no read message in progress
    if(readBlockActive)
    {
      throw new IllegalStateException("Read message active");
    }

    //If not already in produce mode, compact the buffer and flip it so that new data are written to the end.
    if (!produce)
    {
      reclaim();
      produce = true;
    }
  }

  protected void setConsumeMode()
  {
    //We can consume data if there is no write message in progress
    if(writeBlockActive)
    {
      throw new IllegalStateException("Write message active");
    }

    //If not already in consume mode, flip the buffer so that existing data are read from the beginning.
    if (produce)
    {
      internalBuffer.limit(produceIndex);
      internalBuffer.position(consumeIndex);
      produce = false;
    }
  }

  protected void reclaim()
  {
    //Set the limit to the produce index and the position to the consume index, then do a compact operation on the buffer.
    internalBuffer.limit(produceIndex);
    internalBuffer.position(consumeIndex);
    internalBuffer.compact();
    consumeIndex = 0;
    produceIndex = internalBuffer.position();
  }

  /**
   * Return the number of consumable bytes in the buffer.
   *
   * @return
   */
  public int size()
  {
    //Return the difference between produce and consume.
    return produceIndex - consumeIndex;
  }

  //MOVE DATA TO AND FROM COMMUNICATION CHANNELS

  @Override
  public int drainBufferToChannel(WritableByteChannel channel) throws IOException
  {
    try
    {
      //Check we can consume data - no write message is in construction.
      //checkConsumable();

      //Set to produce.
      setConsumeMode();

      //Get the channel data into the buffer.
      int i = channel.write(internalBuffer);

      if (i > 0)
      {
        //Update the produce index.
        consumeIndex += i;
      }

      //Return the number of bytes drained.
      return i;
    }
    finally
    {
      //Release lock.
//      releaseOperationLock();
    }
  }

  @Override
  @Deprecated
  public int drainBufferToChannel(WritableByteChannel channel, int maxBytesToSend) throws IOException
  {
    try
    {
      //Check we can consume data - no write message is in construction.
      //checkConsumable();

      //Set to produce.
      setConsumeMode();

      //Further update the buffer limit to reflect the maxBytesToSend parameter.
      internalBuffer.limit(Math.min(internalBuffer.limit(), consumeIndex + maxBytesToSend));

      //Get the channel data into the buffer.
      int i = channel.write(internalBuffer);

      if (i > 0)
      {
        //Update the produce index.
        consumeIndex += i;
      }

      //Remove the added limit - put the buffer dimensions back to normal.
      internalBuffer.limit(produceIndex);

      //Return the number of bytes drained.
      return i;
    }
    finally
    {
      //Release lock.
//      releaseOperationLock();
    }
  }

  @Override
  public boolean hasDataToConsume()
  {
    return size() > 0;
  }

  @Override
  public int fillBufferFromChannel(ReadableByteChannel channel) throws IOException
  {
    try
    {
      //Check we can produce data - no read message is in construction.
      //checkProducable();

      //Set to produce.
      setProduceMode();

      //Get the channel data into the buffer.
      int i = channel.read(internalBuffer);

      if (i > 0)
      {
        //Update the produce index.
        produceIndex += i;
      }

      //Return the number of bytes filled.
      return i;
    }
    finally
    {
      //Release lock.
//      releaseOperationLock();
    }
  }

  @Override
  public int fillBufferFromChannel(ReadableByteChannel channel, int maxBytesToReceive) throws IOException
  {
    try
    {
      //Check we can produce data - no read message is in construction.
      //checkProducable();

      //Set to produce.
      setProduceMode();

      //Set the
      internalBuffer.limit(Math.min(internalBuffer.capacity(), produceIndex + maxBytesToReceive));

      //Get the channel data into the buffer.
      int i = channel.read(internalBuffer);

      if (i > 0)
      {
        //Update the produce index.
        produceIndex += i;
      }

      //Set the limit back to the capacity.
      internalBuffer.limit(internalBuffer.capacity());

      //Return the number of bytes filled.
      return i;
    }
    finally
    {
    }
  }

  @Override
  public boolean canAcceptTransfer()
  {
    return produceIndex < internalBuffer.capacity();
  }

  @Override
  public boolean hasSpaceFor(int requiredBufferLen)
  {
    return requiredBufferLen <= (internalBuffer.capacity() - produceIndex);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //CONSUME SCALAR DATA FROM THE BUFFER
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /// BYTE DATA

  public byte consumeByte()
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    byte ret = internalBuffer.get();

    //Update the consume pointer.
    this.consumeIndex += 1;

    //Return the data.
    return ret;
  }

  public byte peekByte(int atPosition)
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    byte ret = internalBuffer.get(atPosition + this.consumeIndex);

    //Return the data.
    return ret;
  }


  /// CHAR DATA

  public char consumeChar()
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    char ret = internalBuffer.getChar();

    //Update the consume pointer.
    this.consumeIndex += 2;

    //Return the data.
    return ret;
  }

  public char peekChar(int atPosition)
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    char ret = internalBuffer.getChar(atPosition + this.consumeIndex);

    //Return the data.
    return ret;
  }

  /// SHORT DATA

  public short consumeShort()
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    short ret = internalBuffer.getShort();

    //Update the consume pointer.
    this.consumeIndex += 2;

    //Return the data.
    return ret;
  }

  public short peekShort(int atPosition)
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    short ret = internalBuffer.getShort(atPosition + this.consumeIndex);

    //Return the data.
    return ret;
  }

  /// INT DATA

  public int consumeInt()
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    int ret = internalBuffer.getInt();

    //Update the consume pointer.
    this.consumeIndex += 4;

    //Return the data.
    return ret;
  }

  public int peekInt(int atPosition)
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    int ret = internalBuffer.getInt(atPosition + this.consumeIndex);

    //Return the data.
    return ret;
  }

  /// LONG DATA

  public long consumeLong()
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    long ret = internalBuffer.getLong();

    //Update the consume pointer.
    this.consumeIndex += 8;

    //Return the data.
    return ret;
  }

  public long peekLong(int atPosition)
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    long ret = internalBuffer.getLong(atPosition + this.consumeIndex);

    //Return the data.
    return ret;
  }

  /// FLOAT DATA

  public float consumeFloat()
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    float ret = internalBuffer.getFloat();

    //Update the consume pointer.
    this.consumeIndex += 4;

    //Return the data.
    return ret;
  }

  public float peekFloat(int atPosition)
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    float ret = internalBuffer.getFloat(atPosition + this.consumeIndex);

    //Return the data.
    return ret;
  }

  /// DOUBLE DATA

  public double consumeDouble()
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    double ret = internalBuffer.getDouble();

    //Update the consume pointer.
    this.consumeIndex += 8;

    //Return the data.
    return ret;
  }

  public double peekDouble(int atPosition)
  {
    //Set to consume mode.
    setConsumeMode();

    //Get the next byte from the queue data.
    double ret = internalBuffer.getDouble(atPosition + this.consumeIndex);

    //Return the data.
    return ret;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //PRODUCE SCALAR DATA FROM THE BUFFER
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  ///*** Stream produce - add to next available buffer position.

  /// BYTE DATA

  public PipelineBuffer produceByte(byte data)
  {
    //Set to consume mode.
    setProduceMode();

    //Put the next byte in the data buffer.
    internalBuffer.put(data);

    //Update the consume pointer.
    this.produceIndex += 1;

    //Return the data.
    return this;
  }

  /// CHAR DATA


  public PipelineBuffer produceChar(char data)
  {
    //Set to produce mode.
    setProduceMode();

    //Get the next byte from the queue data.
    internalBuffer.putChar(data);

    //Update the produce pointer.
    this.produceIndex += 2;

    //Return the data.
    return this;
  }

  /// SHORT DATA

  public PipelineBuffer produceShort(short data)
  {
    //Set to produce mode.
    setProduceMode();

    //Get the next byte from the queue data.
    internalBuffer.putShort(data);

    //Update the produce pointer.
    this.produceIndex += 2;

    //Return the data.
    return this;
  }

  /// INT DATA

  public PipelineBuffer produceInt(int data)
  {
    //Set to produce mode.
    setProduceMode();

    //Get the next byte from the queue data.
    internalBuffer.putInt(data);

    //Update the produce pointer.
    this.produceIndex += 4;

    //Return the data.
    return this;
  }

  /// LONG DATA

  public PipelineBuffer produceLong(long data)
  {
    //Set to produce mode.
    setProduceMode();

    //Get the next byte from the queue data.
    internalBuffer.putLong(data);

    //Update the produce pointer.
    this.produceIndex += 8;

    //Return the data.
    return this;
  }

  /// FLOAT DATA

  public PipelineBuffer produceFloat(float data)
  {
    //Set to produce mode.
    setProduceMode();

    //Get the next byte from the queue data.
    internalBuffer.putFloat(data);

    //Update the produce pointer.
    this.produceIndex += 4;

    //Return the data.
    return this;
  }

  /// DOUBLE DATA

  public PipelineBuffer produceDouble(double data)
  {
    //Set to produce mode.
    setProduceMode();

    //Get the next byte from the queue data.
    internalBuffer.putDouble(data);

    //Update the produce pointer.
    this.produceIndex += 8;

    //Return the data.
    return this;
  }

  ///*** Block message produce - set at the buffer location from the start of the message.

  /// BYTE DATA

  public PipelineBuffer produceByteAt(int atPosition, byte data)
  {
    //Make sure there is a write block in progress.
    checkWriteLock();

    //Get the next byte from the queue data.
    internalBuffer.put(atPosition + this.writeMessageStartPosition, data);

    //Return the data.
    return this;
  }


  /// CHAR DATA

  public PipelineBuffer produceCharAt(int atPosition, char data)
  {
    //Make sure there is a write block in progress.
    checkWriteLock();

    //Get the next byte from the queue data.
    internalBuffer.putChar(atPosition + this.writeMessageStartPosition, data);

    //Return the data.
    return this;
  }

  /// SHORT DATA

  public PipelineBuffer produceShortAt(int atPosition, short data)
  {
    //Make sure there is a write block in progress.
    checkWriteLock();

    //Get the next byte from the queue data.
    internalBuffer.putShort(atPosition + this.writeMessageStartPosition, data);

    //Return the data.
    return this;
  }

  /// INT DATA

  public PipelineBuffer produceIntAt(int atPosition, int data)
  {
    //Make sure there is a write block in progress.
    checkWriteLock();

    //Get the next byte from the queue data.
    internalBuffer.putInt(atPosition + this.writeMessageStartPosition, data);

    //Return the data.
    return this;
  }

  /// LONG DATA

  public PipelineBuffer produceLongAt(int atPosition, long data)
  {
    //Make sure there is a write block in progress.
    checkWriteLock();

    //Get the next byte from the queue data.
    internalBuffer.putLong(atPosition + this.writeMessageStartPosition, data);

    //Return the data.
    return this;
  }

  /// FLOAT DATA

  public PipelineBuffer produceFloatAt(int atPosition, float data)
  {
    //Make sure there is a write block in progress.
    checkWriteLock();

    //Get the next byte from the queue data.
    internalBuffer.putFloat(atPosition + this.writeMessageStartPosition, data);

    //Return the data.
    return this;
  }

  /// DOUBLE DATA

  public PipelineBuffer produceDoubleAt(int atPosition, double data)
  {
    //Make sure there is a write block in progress.
    checkWriteLock();

    //Get the next byte from the queue data.
    internalBuffer.putDouble(atPosition + this.writeMessageStartPosition, data);

    //Return the data.
    return this;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //OPERATION LOCKING FOR DELIMITED MESSAGING DATA.
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//  /**
//   * We can consume data if there is no write message in progress.
//   */
//  protected void checkConsumable()
//  {
//    //We can consume data if there is no write message in progress
//    if(writeBlockActive)
//    {
//      throw new IllegalStateException("Write message active");
//    }
//  }
//
//  /**
//   * We can produce data if there is no read message in progress.
//   */
//  protected void checkProducable()
//  {
//    //We can produce data if there is no read message in progress.
//    if(readBlockActive)
//    {
//      throw new IllegalStateException("Read message active");
//    }
//  }
//
//  /**
//   * Check that a read message is in progress.
//   */
//  protected void checkReadLock()
//  {
//    if(!readBlockActive)
//    {
//      throw new IllegalStateException("Read message block operation not active");
//    }
//  }

  /**
   * Check that a write message is in progress.
   */
  protected void checkWriteLock()
  {
    if(!writeBlockActive)
    {
      throw new IllegalStateException("Write message block operation not active");
    }
  }

  /**
   * True if there are no consumable data in the buffer.
   * @return
   */
  public boolean isEmpty()
  {
    return produceIndex == consumeIndex;
  }

  public PipelineBuffer setOrder(ByteOrder order)
  {
    this.internalBuffer.order(order);
    return this;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //FIXED MESSAGE SIZE DELIMITING
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  ///*** READ BLOCK

  /**
   * Try to start a message if there is enough data to satisfy it in the buffer.  If not then return false.
   *
   * @param length
   * @return
   */
  public boolean tryStartReadMessage(int length)
  {
    //Make sure we are consuming.
    setConsumeMode();

    //Do we have enough data to satisfy the entire message?
    if(size() < length) return false;

    //Store the message block end position.
    this.readMessageEndPosition = this.internalBuffer.position() + length;

    //Store the message block start position.
    this.readMessageStartPosition = this.internalBuffer.position();

    //Set the internal buffer limit to position + length.
    this.internalBuffer.limit(this.readMessageEndPosition);

    //Set the read block active flag.
    this.readBlockActive = true;

    //Return this.
    return true;
  }

  public PipelineBuffer startReadMessage(int length)
  {
    //Make sure we are consuming.
    setConsumeMode();

    //Do we have enough data to satisfy the entire message?
    if(size() < length) throw new BufferUnderflowException();

    //Store the message block end position.
    this.readMessageEndPosition = this.internalBuffer.position() + length;

    //Store the message block start position.
    this.readMessageStartPosition = this.internalBuffer.position();

    //Set the internal buffer limit to position + length.
    this.internalBuffer.limit(this.readMessageEndPosition);

    //Set the read block active flag.
    this.readBlockActive = true;

    //Return this.
    return this;
  }

  /**
   * If we are processing message data then the limit has been set to the initial buffer consume position + message length.
   * Return true if there are consumable data remaining.
   *
   * @return
   */
  public boolean hasReadMessageDataRemaining()
  {
    //Throw an exception if we aren't processing a message and this method is called.
    if(!readBlockActive) throw new IllegalStateException();

    //Return the hasRemaining state of the underlying buffer.
    return this.internalBuffer.hasRemaining();
  }

  /**
   * Complete the message block.  This must be called before the buffer can be used for anything other than consuming
   * scalar data from the buffer with the consumeXXX() methods.
   *
   * @return
   */
  public PipelineBuffer completeReadMessage()
  {
    //Restore the buffer limit to the produce index.
    this.internalBuffer.limit(this.produceIndex);

    //Make sure the position is at the message end position - all data are consumed regardless if there are unconsumed
    //data at the end of the message.
    this.internalBuffer.position(this.readMessageEndPosition);

    //Set the read block active flag.
    this.readBlockActive = false;

    //Make the consume index the read message end position.
    this.consumeIndex = this.readMessageEndPosition;

    //Return this.
    return this;
  }

  /**
   * Complete the message block.  This must be called before the buffer can be used for anything other than consuming
   * scalar data from the buffer with the consumeXXX() methods.
   *
   * @return
   */
  public PipelineBuffer rewindReadMessage()
  {
    //Restore the buffer limit to the produce index.
    this.internalBuffer.limit(this.produceIndex);

    //Make sure the position is at the message end position - all data are consumed regardless if there are unconsumed
    //data at the end of the message.
    this.internalBuffer.position(this.readMessageStartPosition);

    //Set the read block active flag.
    this.readBlockActive = false;

    //Make the consume index the read message end position.
    this.consumeIndex = this.readMessageStartPosition;

    //Return this.
    return this;
  }

  ///*** WRITE BLOCK

  public PipelineBuffer startWriteMessage()
  {
    //Make sure we are consuming.
    setProduceMode();

    //Set the write block active flag.
    this.writeBlockActive = true;

    //Store the message block end position.
    this.writeMessageStartPosition = this.internalBuffer.position();

    //Return this.
    return this;
  }

  /**
   * If we are processing message data then the limit has been set to the initial buffer consume position + message length.
   * Return true if there are consumable data remaining.
   *
   * @return
   */
  public boolean hasWriteMessageSpaceRemaining()
  {
    //Throw an exception if we aren't processing a message and this method is called.
    if(!writeBlockActive) throw new IllegalStateException();

    //Return the hasRemaining state of the underlying buffer.
    return this.internalBuffer.hasRemaining();
  }

  public int messageBlockWritePosition()
  {
    //Throw an exception if we aren't processing a message and this method is called.
    if(!writeBlockActive) throw new IllegalStateException();

    //Return the hasRemaining state of the underlying buffer.
    return this.internalBuffer.position() - this.writeMessageStartPosition;
  }

  /**
   * Complete the message block.  This must be called before the buffer can be used for anything other than consuming
   * scalar data from the buffer with the consumeXXX() methods.
   *
   * @return
   */
  public PipelineBuffer completeWriteMessage()
  {
    //Reset the write block active flag.
    this.writeBlockActive = false;

    //Make the produce index the position.
    this.produceIndex = this.internalBuffer.position();

    //Return this.
    return this;
  }

}
