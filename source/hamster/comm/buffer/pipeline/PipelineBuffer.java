package hamster.comm.buffer.pipeline;

import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.buffer.FillableChannelBuffer;
import hamster.comm.buffer.block.itf.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * <p>The general pipeline buffer simply buffers data bytes.  Data can be consumed in defined block using methods from the
 * {@link MessageBlockConsumeProvider}, {@link WholeBufferConsumeProvider} and {@link MessageBlockCalculateProvider} interfaces.
 *
 * @author jdf19
 */
public class PipelineBuffer implements FillableChannelBuffer, DrainableChannelBuffer
{
  /**
   * <p>Buffer object that is wrapped by this utility class.
   */
  private final ByteBuffer internalBuffer;

  //If the produce flag is true then the buffer is to be filled.
  //If the flag is false, the buffer is to be consumed.
  //As the buffer is empty initiall and there is nothing to consume, start off in produce mode.
  private boolean produce = true;

  //Block writer for writing structured data to the internal buffer.
  private SequentialBlockWriter internalWriter;

  //Block reader for reading structure data from the internal buffer.
  private SequentialBlockReader internalReader;

  private int consumeIndex = 0;

  private int produceIndex = 0;

  //Only one operation can take place at once.  Re-entrant calls are not permitted.
  //Otherwise the complexity gets very difficult to manage and this library wants to
  //keep things simple.
  private boolean operationLock = false;

  /**
   * Create an instance of the general pipeline buffer with the given factory and logger instances.
   *
   * @param bufferFact the buffer factory to use when creating an instance of the general pipeline buffer.
   */
  public PipelineBuffer(PipelineBufferFactory bufferFact)
  {
    internalBuffer = bufferFact.newBuffer();

    internalWriter = new SequentialBlockWriter(internalBuffer);

    internalReader = new SequentialBlockReader(internalBuffer);
  }

  public PipelineBuffer produceFromBytes(byte[] bytesToTransferToThisBuffer)
  {
    try
    {
      //Check lock.
      checkOperationLock();

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
      releaseOperationLock();
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
      //Check lock.
      checkOperationLock();

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
      releaseOperationLock();
    }
  }

  /**
   * Produce data from a {@link PipelineProducer} callback.  This callback is used to produce structured data into
   * this buffer instance.
   *
   * @param producer
   * @return
   */
  public PipelineBuffer produceData(PipelineProducer producer)
  {
    try
    {
      //Check lock.
      checkOperationLock();

      //Set to produce.
      setProduceMode();

      //Produce to the given reference.
      producer.produceToBuffer(internalWriter);

      //Update the produce index.
      produceIndex = internalBuffer.position();

      //Return this.
      return this;
    }
    finally
    {
      //Release lock.
      releaseOperationLock();
    }
  }

  public PipelineBuffer produceFromByteBuffer(ByteBuffer producer)
  {
    try
    {
      //Check lock.
      checkOperationLock();

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
      releaseOperationLock();
    }
  }

  public PipelineBuffer produceDataFromPipelineBuffer(PipelineBuffer producer)
  {
    try
    {
      //Check lock.
      checkOperationLock();

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
      releaseOperationLock();
    }

  }

  private void transferToTargetBuffer(PipelineBuffer target)
  {
    try
    {
      //Check lock.
      checkOperationLock();

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
      releaseOperationLock();
    }
  }

  public PipelineBuffer consumeBytes(byte[] bytesToTransferToThisBuffer, int startIx, int length)
  {
    try
    {
      //Check lock.
      checkOperationLock();

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
      releaseOperationLock();
    }
  }

  public PipelineBuffer consumeBytes(byte[] bytesToTransferToThisBuffer)
  {
    try
    {
      //Check lock.
      checkOperationLock();

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
      releaseOperationLock();
    }
  }

  public int consumeData(PipelineConsumer consumer)
  {
    try
    {
      //Check lock.
      checkOperationLock();

      //Set to consume.
      setConsumeMode();

      //Set the start position.  If the block data are not consumed, we will return to this buffer position.
      int startBufferPosition = internalBuffer.position();

      //Consume.
      int blockDataWereConsumed = consumer.consumeFromBuffer(internalReader.setBlock());

      //Make sure they aren't doing anything nefarious or foolish.
      if ((blockDataWereConsumed < 0) || (blockDataWereConsumed > size()))
        throw new IllegalCallerException();//TODO message

      //Set the position.
      internalBuffer.position(startBufferPosition + blockDataWereConsumed);
      //Update the consume index.
      consumeIndex = internalBuffer.position();

      //Return the block consumed status.
      return blockDataWereConsumed;
    }
    finally
    {
      //Release lock.
      releaseOperationLock();
    }
  }

  protected void setProduceMode()
  {
    //If not already in produce mode, compact the buffer and flip it so that new data are written to the end.
    if (!produce)
    {
      reclaim();
      produce = true;
    }
  }

  protected void setConsumeMode()
  {
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
      //Check lock.
      checkOperationLock();

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
      releaseOperationLock();
    }
  }

  @Override
  @Deprecated
  public int drainBufferToChannel(WritableByteChannel channel, int maxBytesToSend) throws IOException
  {
    try
    {
      //Check lock.
      checkOperationLock();

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
      releaseOperationLock();
    }
  }

  @Override
  public boolean hasDataToConsume()
  {
    return size() > 0;
  }

  @Override
  public int fromChannel(ReadableByteChannel channel) throws IOException
  {
    try
    {
      //Check lock.
      checkOperationLock();

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
      releaseOperationLock();
    }
  }

  @Override
  public int fromChannel(ReadableByteChannel channel, int maxBytesToReceive) throws IOException
  {
    try
    {
      //Check lock.
      checkOperationLock();

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
      //Release lock.
      releaseOperationLock();
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

  /**
   * Check the operation lock is false and set it to true if so.  If it is true then throw an illegal state
   * exception.
   *
   * @throws IllegalStateException if there is a reentrant call.
   */
  private void checkOperationLock()
  {
    if (operationLock)
    {
      throw new IllegalStateException();
    }

    //Set the operation lock.
    operationLock = true;
  }

  /**
   * Release the operation lock.  It must be set or else we have an illegal state.
   */
  private void releaseOperationLock()
  {
    if (!operationLock)
    {
      throw new IllegalStateException();
    }

    //Set the operation lock.
    operationLock = false;
  }

  /**
   * True if there are no consumable data in the buffer.
   * @return
   */
  public boolean isEmpty()
  {
    return produceIndex == consumeIndex;
  }
}
