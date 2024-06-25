package hamster.comm.buffer.block;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ResourceBundle;

import org.slf4j.Logger;

import hamster.comm.buffer.block.itf.AcceptingChannelBuffer;
import hamster.comm.buffer.block.itf.ConsumableCalculationBlockHandler;
import hamster.comm.buffer.block.itf.ReadAllHandler;
import hamster.comm.buffer.block.itf.ReadBlockHandler;
import hamster.comm.buffer.block.itf.ReadableCompletionBlockHandler;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.buffer.factory.GrowUpdateExpandableBufferFactory;
import hamster.comm.logging.DummyLogger;

/**
 * <p>
 * A buffer object that supports atomic operations. It receives data and
 * provides data in user-defined blocks.
 * <p>
 * This class is meant to be used as a building block in other classes; it
 * supports the notions of filling, producing, draining and consuming but
 * delegates the actual buffer transfer to subclasses.
 * <p>
 * When consuming data in a block, ALL DATA MUST BE CONSUMED in the call.  Not doing so, even if it does not cause an immediate problem,
 * can introduce bugs if allowed.  An exception will be thrown if a consume block operation does not fully consume the data in the block.
 * <p>
 * This class is definitely not thread-safe; it MUST be used in a
 * single-threaded environment.
 * 
 * @author jdf19
 *
 */
public class BlockBufferBase implements AcceptingChannelBuffer
{
  /**
   * <p>The buffer can be in either PRODUCE or CONSUME mode at any one time.  The mode that
   * the buffer is in affects how the position and limit of the underlying ByteBuffer are set. 
   */
  private enum MODE
  {
    PRODUCE,
    CONSUME
  }
  
  /**
   * <p>The mode.  Initialise to PRODUCE; we can't CONSUME data unless there is some available.
   */
  private MODE mode = MODE.PRODUCE;
  
  /**
   * <p>Logger object for reporting rebuffering.
   */
  private final Logger logger;
  
  /**
   * <p>
   * The internal consume buffer.  The visibility is <code>protected</code>, which requires extreme caution.
  * It is accessible to subclasses primarily because of encryption extensions, which require references
  * to a source and destination ByteBuffer.
   */
  protected ByteBuffer thisBuffer;
  
  /**
   * <p>
   * Message block reader will be given to callers of the
   * {@link PipelineBuffer#produceFromHandler(WritableBlockCallback)} method to
   * consume block data.
   */
  private final SequentialMessageBlockWriter writer;

  /**
   * <p>
   * Message block reader will be given to callers of the
   * {@link PipelineBuffer#consumeToHandler(ReadableBlockCallback, ReadableBlockCompleteCalculator)}
   * method to consume block data.
   */
  private final SequentialMessageBlockReader reader;
  
  /**
   * <p>Lock the buffer against reentrant modification.  One operation must complete before another begins.  Reentrant produce and consume operations
   * are allowed but once a consume has started then only reentrant consumes will be allowed and no other operation like fill, drain or produce.
   * Same goes for produce operations.
   */
  private boolean operationLock = false;
  
  /**
   * <p>Reentrant consume level count.
   */
  private int consumeRecursionLevel = 0;

  /**
   * <p>Reentrant produce level count.
   */
  private int produceRecursionLevel = 0;
  
  /**
   * <p>Buffer factory - expand storage when required.
   */
  //private final GrowUpdateExpandableBufferFactory bufferFact;
  
  /**
   * <p>Buffer instance ID for reporting.
   */
  private final String bufferId;
  
  /**
   * <p>Owner-updatable consume position.  Buffer reclaim is a manual operation, so we need to use the consume position pointer
   * to keep track of the start of as yet unconsumed data.
   */
  private int consumePosition = 0;

  /**
   * <p>Produce position, will be moved forward as data are produced or filled into the buffer.  Will be moved back in response to 
   * a reclaim operation.
   * 
   * <p>Protected access so use with extreme caution.  Manipulation of produce position may be necessary for subclasses. 
   */
  private int producePosition = 0;

  /**
   * <p>Buffer factory will be used to rebuffer.
   */
  private final GrowUpdateExpandableBufferFactory factory;
  
  /**
   * <p>
   * Construct an instance of the pipeline buffer. Use the buffer factory to
   * re-buffer if necessary.
   * 
   * @param bufferFact the buffer factory to use to construct this instance.
   * @param bufferId the buffer descriptive identifier to use for logging operations.
   * @param logger the logger to log buffer operations to.  This is useful for debugging but the {@link BlockBufferBase#BlockBufferBase(GrowUpdateExpandableBufferFactory, String)} method should be preferred in production systems.
   */
  public BlockBufferBase(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
  {
    //Assign the logger.
    this.logger = (logger != null) ? logger : new DummyLogger();
    
    //Assign the buffer id.
    this.bufferId = bufferId;
    
    // Create the block reader.
    reader = new SequentialMessageBlockReader();

    // Create the block writer.
    writer = new SequentialMessageBlockWriter(bufferFact, new ClosedInternalRebufListener());
    
    //Store reference to factory.
    this.factory = bufferFact;
  }
  
  /**
   * <p>
   * Construct an instance of the pipeline buffer. Use the buffer factory to
   * re-buffer if necessary.
   * <p>No logger is provided so an internal dummy logger will be used.
   * 
   * @param bufferFact the buffer factory to use to construct this instance.
   * @param bufferId the buffer descriptive identifier to use for logging operations.
   */
  public BlockBufferBase(GrowUpdateExpandableBufferFactory bufferFact, String bufferId)
  {
    //Dummy logger.
    this.logger = new DummyLogger();
    
    //Assign the buffer id.
    this.bufferId = bufferId;
    
    // Create the block reader.
    reader = new SequentialMessageBlockReader();

    // Create the block writer.
    writer = new SequentialMessageBlockWriter(bufferFact, new ClosedInternalRebufListener());
    
    //Store reference to factory.
    this.factory = bufferFact;
  }

  /**
   * <p>Fill data from the incoming data channel.
   * 
   * @param incomingDataChannel the readable byte channel to use as a source when producing data to this buffer. 
   * @return the number of bytes that were filled from the given channel.
   * @throws IOException if the transfer operation raised a lower-level exception
   */
  public int fillData(ReadableByteChannel incomingDataChannel) throws IOException
  {
    try
    {
      // Filling is not a re-entrant operation.
      if(operationLock) throw new IllegalStateException();

      //Reclaim space prior to filling.  Reclaim all consumed data in consume buffer.
      doReclaim(consumePosition);
      
      //Set the operation lock.
      operationLock = true;

      //If we're in CONSUME mode then switch the buffer to PRODUCE.
      switchProduce();
      
      // Read the data.
      int i = incomingDataChannel.read(thisBuffer);

      //If i == -1 then the channel is closed for reading.  Do no processing.
      if(i != -1)
      {
        //Produce position.
        producePosition += i;
        
        //If the buffer has been filled by this operation then expand it.
        if(!thisBuffer.hasRemaining())
        {
          ByteBuffer newBuffer = factory.replaceBuffer(thisBuffer);
          
          //Update listener.
          doRebuffer(newBuffer);
        }
      }
      
      // Return the number of bytes read - could be -1 or 0.
      return i;
    }
    finally
    {
      //Release the operation lock.
      operationLock = false;
    }
  }

  /**
   * <p>Fill data from the incoming data channel.
   * 
   * @param incomingDataChannel the readable byte channel to use as a source when producing data to this buffer. 
   * @param maxBytes the maximum number of bytes to transfer.  If there are fewer bytes in the incoming data channel than this value then all available bytes will be transferred.  There is no requirement for the whole number of bytes to be transferred. 
   * @return the number of bytes that were filled from the given channel.
   * @throws IOException if the transfer operation raised a lower-level exception
   */
  public int fillData(ReadableByteChannel incomingDataChannel, int maxBytes) throws IOException
  {
    try
    {
      // Filling is not a re-entrant operation.
      if(operationLock) throw new IllegalStateException();

      //Reclaim space prior to filling.  Reclaim all consumed data in consume buffer.
      doReclaim(consumePosition);
      
      //Set the operation lock.
      operationLock = true;

      //If we're in CONSUME mode then switch the buffer to PRODUCE.
      switchProduce();
      
      //Rebuffer if not enough space to receive the maximum message.
      if(thisBuffer.remaining() < maxBytes)
      {
        //Just expand it by max bytes.  Should poss do maxBytes - thisBuffer.remaining() but so what.
        ByteBuffer newBuffer = factory.replaceBuffer(thisBuffer, maxBytes);
        
        //Update listener.
        doRebuffer(newBuffer);
      }
      
      //Set the limit.
      thisBuffer.limit(thisBuffer.position() + maxBytes);
      
      // Read the data.
      int i = incomingDataChannel.read(thisBuffer);

      //If i == -1 then the channel is closed for reading.  Do no processing.
      if(i != -1)
      {
        //Restore the limit.
        thisBuffer.limit(thisBuffer.capacity());

        //Produce position.
        producePosition += i;
      }
      
      // Return the number of bytes read - could be -1 or 0.
      return i;
    }
    finally
    {
      //Release the operation lock.
      operationLock = false;
    }
  }

  /**
   * <p>
   * Drain all available data to the given channel, which will receive as much as
   * it has space for.
   * 
   * @param outgoingDataChannel the writable byte channel to use as a destination when consuming data from this buffer.
   * @return the number of bytes that were drained from this buffer.
   * @throws IOException if the transfer operation raised a lower-level exception
   */
  public int drainData(WritableByteChannel outgoingDataChannel) throws IOException
  {
    // Read the data.
    try
    {
      // Filling is not a re-entrant operation.
      if(operationLock) throw new IllegalStateException();

      //Set the operation lock.
      operationLock = true;

      //If we're in CONSUME mode then switch the buffer to PRODUCE.
      switchConsume();

      //Write consumable data to channel.
      int i = outgoingDataChannel.write(thisBuffer);

      //Advance the consume position.
      if(i > 0) consumePosition += i;
      
      // Return the number of bytes read - could be -1 or 0.
      return i;
    }
    finally
    {
      //Release the operation lock.
      operationLock = false;
    }
  }

  /**
   * <p>
   * Drain the maximum amount of data to the given channel, which will receive as
   * much as it has space for upto this limit. If the buffer doesn't contain
   * enough bytes to satisfy the maximum condition then it will attempt to drain
   * as much as it has.
   * 
   * @param outgoingDataChannel the writable byte channel to use as a destination when consuming data from this buffer.
   * @param maxToDrain the maximum number of bytes to drain to the target buffer.  If fewer bytes than requested are available in this buffer them all available bytes in this buffer will be transferred. 
   * @return the number of bytes that were drained from this buffer.
   * @throws IOException if the transfer operation raised a lower-level exception
   */
  public int drainData(WritableByteChannel outgoingDataChannel, int maxToDrain) throws IOException
  {
    // Draining is not a re-entrant operation.
    if(operationLock) throw new IllegalStateException();

    //Set the operation lock.
    operationLock = true;

    //If we're in CONSUME mode then switch the buffer to PRODUCE.
    switchConsume();

    //Calculate the buffer limit.  It can't be greater than write position (the extent of readable data).
    int drainCalc = Math.min(maxToDrain, consumableBytesInBuffer());

    try
    {
      //Temporarily set limit on consume buffer.
      int bufferLim = thisBuffer.limit();
      thisBuffer.limit(Math.min(bufferLim, drainCalc));
      
      // Write to the channel.
      int i = outgoingDataChannel.write(thisBuffer);

      //Restore the consume buffer limit to the produce buffer position (the max produced data).
      thisBuffer.limit(bufferLim);
      
      //Advance the consume position.
      if(i > 0) consumePosition += i;
      
      // Return the number of bytes read - could be -1 or 0.
      return i;
    }
    finally
    {
      //Release the operation lock.
      operationLock = false;
    }
  }

  /**
   * <p>
   * Produce an entire message from the given callback.
   * 
   * <p>Producing is not a re-entrant operation.  If blocks-within-blocks are required, use the {@link SequentialMessageBlockWriter#addWritableBlock(WriteBlockHandler)}
   * method to add sub-blocks.
   * 
   * @param callback the source for data to produce to this buffer.
   * @return the number of bytes that were produced to this buffer.
   */
  public int produceData(WriteBlockHandler callback)
  {
    // Check the callback for null.
    if (callback == null) throw new NullPointerException();

    // Producing is not a re-entrant operation.
    if((produceRecursionLevel == 0) && operationLock) throw new IllegalStateException();
        
    //Reclaim buffer space before producing.
    if(produceRecursionLevel == 0) doReclaim(consumePosition);
    
    //Increment produce recursion level.
    produceRecursionLevel++;
    
    //If we're in CONSUME mode then switch the buffer to PRODUCE.
    switchProduce();

    //Start the writer block.
    writer.start();
    
    try
    {      
      //Set the operation lock.
      operationLock = true;
      
      //Save the buffer position.  We want to use it to calculate how many bytes have been produced.
      int bufPosition = thisBuffer.position();
      
      // Consume message block.
      callback.writeMessageBlock(writer);
      
      //Number of bytes to advance produce position.
      int opBytesWritten = (thisBuffer.position() - bufPosition);
      
      //Produce position.
      producePosition += opBytesWritten;
      
      //Return number of bytes produced.
      return opBytesWritten;
    }
    finally
    {
      //Decrement the recursion level.
      operationLock = false;
      
      //End writing.
      writer.end();

      //Decrement produce recursion level.
      produceRecursionLevel--;
    }
  }

  /**
   * <p>
   * Check that the next message block is ready to consume. If it is, then call
   * the consumer callback which will read the message data block.
   * <p>
   * Any data that are not consumed will be discarded and the buffer will be ready
   * to attempt to consume the next data message block or perform any other atomic
   * operation.
   * 
   * <p>Consuming is not a re-entrant operation.  If blocks-within-blocks are required, use the {@link SequentialMessageBlockReader#processReadBlockHandler(ReadBlockHandler)}
   * method to consume sub-blocks.
   * 
   * @param completeCalc determines whether or not the message is complete.
   * @return true if the message was consumed.
   */
  public boolean consumeData(ReadableCompletionBlockHandler completeCalc)
  {
    // Check the callback for null.
    if (completeCalc == null) throw new NullPointerException();

    //Not a re-entrant operation!
    if((consumeRecursionLevel == 0) && operationLock) throw new IllegalStateException();
    
    //Increment produce recursion level.
    consumeRecursionLevel++;
    
    //If we're in PRODUCE mode then switch the buffer to CONSUME.
    switchConsume();
    
    //Get the existing block start position for the reader (could be -1 if no block has previously been started).
    int saveStartBlockPos = reader.blockStartPosition();
    
    int messageBlockLen;
    int msgLim = 0;
    boolean processed = false;
    
    //Get the current consume position.
    int curConsumePos = thisBuffer.position();
    
    try
    {
      //Increment the produce recursion level.
      //consumeRecursionLevel++;
      operationLock = true;
      
      //Start a message.
      reader.startBlock(curConsumePos);
      
      //Look at the message to see if it is complete.
      messageBlockLen = (consumableBytesInBuffer() < completeCalc.minimumBytes()) ? -1 : completeCalc.messageBlockCompleteLength(reader);
      
      //Move back to the consume position at the start of the message block complete check.
      thisBuffer.position(curConsumePos);
      
      // If block len > 0 then block is ready. Call the callback to consume the
      // message block.
      //Check that we've got enough in the buffer to satisfy the whole message block whose length is specified by messageBlockLen.      
      if ( (messageBlockLen > 0) && (messageBlockLen <= consumableBytesInBuffer()) )
      {        
        //Message limit.
        msgLim = curConsumePos + messageBlockLen;
        
        //Set the limit on the consume buffer to reflect the message length.
        thisBuffer.limit(msgLim);
        
        //Consume message block data.
        processed = completeCalc.readMessageBlock(reader); 

        if(processed)
        {          
          //Advance consumable buffer to end of consumed message block.
          thisBuffer.position(msgLim);
        }
        else
        {
          //Reset consumable buffer position to start of message block.
          //No message consumption has taken place.
          thisBuffer.position(curConsumePos);
        }
        
        //Advance the consume position.
        consumePosition = thisBuffer.position();
        
        //Reinstate the limit.
        //thisBuffer.limit(saveLimit);
        thisBuffer.limit(producePosition);
        
        // Return the processed flag - whether the message was consumed.
        return processed;
      }
      else
      {
        // No message available for consumption.
        return false;
      }
    }
    catch(Throwable t)
    {
      //Move back to the consume position at the start of the message block.
      thisBuffer.position(curConsumePos);
      
      //Rethrow.
      throw t;
    }
    finally
    {
      //Decrement the produce recursion level.
      operationLock = false;

      //Restore the start block position.
      reader.startBlock(saveStartBlockPos);
      
      //Decrement produce recursion level.
      consumeRecursionLevel--;
    }

  }
  
  /**
   * <p>Allow general calculation of the readable data in the buffer.  Starts from the consume position (which can be set via the 
   * {@link #locateReadPos(int)} method) and ends at the full extent of the readable data in the buffer.
   * 
   * @param handler the handler to consume data from.
   */
  public void runReadableBlockCalculation(ConsumableCalculationBlockHandler handler)
  {
    //Switch consume.
    switchConsume();
    
    //Start the reader block at the current consume position.
    reader.startBlock(thisBuffer.position());
    
    try
    {
      //Call the handler.
      handler.calculateBlock(reader);
    }
    catch(IllegalStateException e)
    {
      //Re-throw exception.
      throw e;
    }
    finally
    {
      //Reset the reader block and set the mode to NONE.
      reader.end();
    }
  }
    
  /**
   * <p>This is like the {@link #runReadableBlockCalculation(ConsumableCalculationBlockHandler)} method above, but it automatically
   * runs the {@link ConsumableCalculationBlockHandler} parameter with all data in the buffer including any that have already been
   * consumed.
   * 
   * @param handler the handler to consume data from.
   */
  public void scanConsumable(ConsumableCalculationBlockHandler handler)
  {
    //Switch consume.
    switchConsume();
    
    //Set buffer pos to 0.
    thisBuffer.position(0);
    
    //Start the reader block at the current consume position.
    reader.startBlock(thisBuffer.position());
    
    try
    {
      //Call the handler.
      handler.calculateBlock(reader);
    }
    catch(IllegalStateException e)
    {
      //Rethrow exception.
      throw e;
    }
    finally
    {
      //Reset the reader block and set the mode to NONE.
      reader.end();

      //Set buffer pos to consume position.
      thisBuffer.position(consumePosition);
    }
  }
  
  /**
   * <p>Set the whole buffer as consumable and call the block handler to consume it.
   * 
   * @param completeCalc transfer all buffer data into this object.
   */
  public void consumeAllData(ReadAllHandler completeCalc)
  {
    // Check the callback for null.
    if (completeCalc == null) throw new NullPointerException();

    //This is a stand-alone operation.  It can't be recursively nested inside any other operation.
    if( operationLock ) throw new IllegalStateException();
    
    //Switch to consume.
    switchConsume();
        
    try
    {
      //Set the whole buffer as readable.
      this.thisBuffer.position(0);
      
      //Set up the reader and call the handler.
      reader.startBlock(0);
      completeCalc.readMessageAll(reader);
      
      //Advance the consume position.
      consumePosition = thisBuffer.position();
    }
    finally
    {
      //Unset the operation lock.
      operationLock = false;
    }
  }

  /**
   * <p>The difference between this method and the {@link #consumeAllData(ReadAllHandler)} method is that
   * calling this method doesn't reset the consumable position to the start of the buffer.
   * 
   * @param handler transfer all buffer data into this object.
   */
  public void consumeNextData(ReadAllHandler handler)
  {
    // Check the callback for null.
    if (handler == null) throw new NullPointerException();

    //This is a stand-alone operation.  It can't be recursively nested inside any other operation.
    if( operationLock ) throw new IllegalStateException();
    
    //Switch to consume.
    switchConsume();
        
    try
    {
      //Set up the reader and call the handler.
      reader.startBlock(consumePosition);
      handler.readMessageAll(reader);
      
      //Advance the consume position.
      consumePosition = thisBuffer.position();
    }
    finally
    {
      //Unset the operation lock.
      operationLock = false;
    }
  }
  
  /**
   * <p>Transfer data into this buffer instance from the given block reader.  All available data in the block reader will be transferred.
   * 
   * @param blockReader the reader to transfer data from.
   */
  public void transferFrom(SequentialMessageBlockReader blockReader)
  {
    //Reclaim from the read position.
    doReclaim(consumePosition);

    //Lock operation.
    if( operationLock ) throw new IllegalStateException();

    //Increment produce recursion level.
    operationLock = true;
    
    //Switch to PRODUCE mode - we're going to produce data.
    switchProduce();
    
    //Start.
    writer.start();
    
    try
    {
      //Save the start position to calculate the number of bytes added.
      int savePos = thisBuffer.position();
      
      //Write the block of data.
      blockReader.writeMessageBlock(writer);
      
      //Update the produce position.
      producePosition += (thisBuffer.position() - savePos);
    }
    catch(Throwable t)
    {
      throw t;
    }
    finally
    {
      //Reduce recursion level.
      operationLock = false;
      
      //End writing.
      writer.end();
    }
    
  }

  /**
   * <p>Transfer data from this buffer instance to the given block writer.  All available data in the buffer will be transferred to the writer.
   * 
   * @param blockReader the reader to transfer data from.
   */
  public void transferTo(SequentialMessageBlockWriter blockWriter)
  {
    //Switch consume.
    switchConsume();
    
    //Start the reader block at the current consume position.
    reader.startBlock(thisBuffer.position());
    
    try
    {
      //Call the handler.
      reader.writeMessageBlock(blockWriter);
    }
    catch(IllegalStateException e)
    {
      //Re-throw exception.
      throw e;
    }
    finally
    {
      //Reset the reader block and set the mode to NONE.
      reader.end();
    }
  }

  /**
   * 
   * @param src
   * @return
   */
  public int transferFromSourceBuffer(ByteBuffer src)
  {
    //Reclaim from the read position.
    doReclaim(consumePosition);

    //Lock operation.
    if( operationLock ) throw new IllegalStateException();

    //Increment produce recursion level.
    operationLock = true;
    
    //Switch to PRODUCE mode - we're going to produce data.
    switchProduce();
    
    //Start.
    writer.start();
    
    try
    {
      //Number of bytes transferred from source.
      int bytesTransferred = 0;
      
      
      bytesTransferred = writer.transferByteBufferContent(src);
//      //Save the start position to calculate the number of bytes added.
//      int savePos = thisBuffer.position();
//      
//      //Write the block of data.
//      blockReader.writeMessageBlock(writer);
//      
//      //Update the produce position.
//      producePosition += (thisBuffer.position() - savePos);
      
      //Return number of bytes transferred.
      return bytesTransferred;
    }
    catch(Throwable t)
    {
      throw t;
    }
    finally
    {
      //Reduce recursion level.
      operationLock = false;
      
      //End writing.
      writer.end();
    }
  }

  // ANCILLARY AND SUPPORT METHODS.

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAcceptTransfer()
  {
    return hasSpaceFor(1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasSpaceFor(int requiredBufferLen)
  {
    // Reclaim any buffer space before calculating this.
    //doReclaim(consumePosition);

    // Check we have capacity in the buffer to transfer the required number of bytes
    // in.
    // We can use a simple call to thisBuffer.remaining() as we have switched into PRODUCE mode int the call above.
    //boolean hasSpace = thisBuffer.remaining() >= requiredBufferLen;
    boolean hasSpace = (thisBuffer.capacity() - producePosition) >= requiredBufferLen;
    return hasSpace;
  }

  /**
   * <p>Allow subclasses to locate the read position, for data resending etc.
   * 
   * @param readPos the buffer position to set the {@link #consumePosition} pointer to.
   * @throws IllegalArgumentException if the parameter is outside the existing buffer constraints.
   */
  public void locateReadPos(int readPos)
  {
    //Check the parameter for valaditidity.
    if( (readPos < 0) || (readPos > producePosition) ) throw new IllegalArgumentException(); 
    
    //Set the read position.
    consumePosition = readPos;
    
    //If we are in CONSUME mode then we also need to update the buffer position.
    if(mode == MODE.CONSUME) thisBuffer.position(consumePosition);
  }

  /**
   * <p>Reclaim the given number of bytes from the front of the buffer.
   * 
   * @param endReclaimPosition start reclaiming from position 0, finish reclaiming from the given end position.
   */
  public void doReclaim(int endReclaimPosition)
  {
    //Default behaviour: handle reclaim operation.
    handleReclaimOperation(endReclaimPosition);
  }
  
  /**
   * <p>Reclaim all consumed bytes from the front of the buffer.
   */
  public void doReclaimAll()
  {
    //Default behaviour: handle reclaim operation.
    if(consumePosition > 0) handleReclaimOperation(consumePosition);
  }

  /**
   * <p>Reclaim the given number of bytes from the front of the buffer.
   * 
   * @param endReclaimPosition start reclaiming from position 0, finish reclaiming from the given end position.
   */
  protected void handleReclaimOperation(int endReclaimPosition)
  {
    //MUST ONLY RECLAIM when there is no produce or consume operation in progress.
    if( operationLock ) throw new IllegalStateException();

    //This can be called with a reclaim position of 0, which gives us nothing to do.  Return immediately if this is the case.
    if(endReclaimPosition == 0) return;
    
    //We are going to take the requested number of bytes from the front of the buffer.  Firstly, check that there are enough
    //bytes in the buffer to complete the request.
    if( (endReclaimPosition > producePosition) ) throw new IllegalArgumentException();
    
    //Set up the buffer for a compact operation.
    thisBuffer.position(endReclaimPosition);
    thisBuffer.limit(producePosition);
    //Compact operation.
    thisBuffer.compact();
    
    //Move the consume and produce position backwards by the number of bytes reclaimed.
    producePosition -= endReclaimPosition;
    consumePosition -= endReclaimPosition;

    //Check the consume position.  If we've reclaimed data that we haven't yet consumed then this needs to be adjusted to 0.
    if(consumePosition < 0) consumePosition = 0;
    
    //Position is now what was the limit and the limit is capacity.  We need to set up the pos and lim depending on what the MODE is.
    if(mode == MODE.PRODUCE)
    {
      //PRODUCE mode.
      //Buffer pos is produce position.  Limit is capacity.
      thisBuffer.limit(thisBuffer.capacity());
      thisBuffer.position(producePosition);
    }
    else
    {
      //CONSUME mode.
      //Buffer pos is consume position.  Limit is produce position.
      thisBuffer.limit(producePosition);      
      thisBuffer.position(consumePosition);
    }
  }
  
  /**
   * <p>
   * Return the number of consumable bytes in the buffer.  This is the difference between the number of bytes that are available to consume,
   * and the number of bytes that have already been consumed.
   * 
   * @return the number of consumable bytes in the buffer.
   */
  public int consumableBytesInBuffer()
  {
    //MUST ONLY check when there is no produce operation in progress.
    if( writer.isValid() ) throw new IllegalStateException();
    
    
    //Consumable number of bytes in buffer is always produce pos - consume pos.
    return producePosition - consumePosition;
  }

  /**
   * <p>
   * Return the number of producible bytes in the buffer.  This is the difference between the number of bytes that have been produced,
   * and the capacity of the buffer.
   * 
   * @return the number of producible bytes in the buffer.
   */
  public int producableBytesInBuffer()
  {
    //MUST ONLY check when there is no produce operation in progress.
    if( reader.isValid() ) throw new IllegalStateException();
    
    
    //Consumable number of bytes in buffer is always produce pos - consume pos.
    return thisBuffer.capacity() - producePosition;
  }

  /**
   * <p>
   * Test for readable bytes in the buffer.
   * 
   * @return true if there are readable byes in the buffer.
   */
  protected boolean hasDataToConsume()
  {
    // Test diff between write and read positions is greater than 0.
    return consumableBytesInBuffer() > 0;
  }

  /**
   * <p>
   * Return the <b>current</b> capacity of the buffer. This buffer can grow in
   * response to produced data.
   * 
   * @return the number of bytes of the current buffer capacity.
   */
  public int getCapacity()
  {
    return thisBuffer.capacity();
  }

  /**
   * <p>
   * Clear ALL data in buffer and completely re-initialise.
   */
  public void clearAll()
  {
    if( operationLock ) 
    {
      //Can't clear while operation locked - this is a bad reentrant call.  Throw an exception.
      throw new IllegalStateException();
    }
    
    // Reset the underlying buffer.
    thisBuffer.clear();
    
    //Consume position to zero.
    consumePosition = 0;
    
    //Produce position to zero.
    producePosition = 0;
    
    //Set to PRODUCE mode.
    mode = MODE.PRODUCE;
  }

  /**
   * <p>Stringify the block buffer base.
   */
  @Override
  public String toString()
  {
    return outputBuffer();//(thisBuffer != null) ? "[av:" + consumableBytesInBuffer() + "|fp:" + producePosition + "|rp:" + consumePosition + "]" : "[init]";
  }

  /**
   * <p>Perform a rebuffering operation.
   * @param newBuf
   */
  private final void doRebuffer(ByteBuffer newBuf)
  {
    //REBUFFERING MUST TAKE PLACE IN PRODUCE MODE ONLY!!!!
    //IF WE ARE NOT IN PRODUCE MODE THEN THIS IS AN ILLEGAL STATE.
    if(mode != MODE.PRODUCE) throw new IllegalStateException();
    
    if(logger.isInfoEnabled() && (thisBuffer != null)) logger.trace(ResourceBundle.getBundle("hamster.comm.i18n.strings").getString("hamster.comm.00000001"), thisBuffer.capacity(), thisBuffer.capacity(), newBuf.capacity());

    // Set this buffer reference to the newly-created buffer.  That newly-created buffer contains the same data and pos/limit pointers as the old one, but
    // a greater capacity.
    thisBuffer = newBuf;

    //Update the reader with the buffer instance.
    reader.updateBuffer(thisBuffer);
  }
  
  /**
   * <p>
   * Closed internal rebuffering listener. We catch the rebuffer at this class level
   * before passing it up to the owner.  Created by default when there is no subclass.
   * 
   * @author jdf19
   *
   */
  private final class ClosedInternalRebufListener implements RebufferingListener
  {
    private ClosedInternalRebufListener()
    {
      // Default constructor - nothing to do.
    }
    
    private ClosedInternalRebufListener(RebufferingListener notifier)
    {
      // Notify
    }

    /**
     * <p>The writer will rebuffer if the produced message overruns the existing buffer space.
     * We clone this for the consume buffer.
     */
    @Override
    public void handleRebuffer(ByteBuffer newBuf)
    {
      //Do the rebuffer.
      doRebuffer(newBuf);
      
      //Notify of a rebuffering.
      notifyHandleRebuffer(newBuf);
    }
  }

  /**
   * <p>Output a readable string of the buffer contents.  Note that this is useful for debugging only and will slow things down if used in production.
   * 
   * @return a readable string of the buffer contents.
   */
  public String outputBuffer()
  {
    if((reader == null) || (reader.thisBuffer == null)) return "[ini]";
    
    StringBuilder sb = new StringBuilder();
    
    //Save the lim which may have been set up as the bounds for a currently-processed message.
    int savelim = thisBuffer.limit();
    
    //Set the limit to capacity for the purposes of printing the buffer out.
    thisBuffer.limit(thisBuffer.capacity());
    
    //PRODUCE POSITION.
    //This operation can be reentrant so we may be in the process of message production (called from a callback method where the writer is in use).
    //If the writer is valid (block start pos != -1) then take the produce position from the buffer position.  If not, use producePosition as
    //there is no write op in progress.
    
    int procPos = (writer.isValid() ? thisBuffer.position() : producePosition);
    
    sb.append(bufferId);
    sb.append(" <pos:");
    sb.append(consumePosition);
    sb.append("|lim:");
    sb.append(procPos);
    sb.append("> ");
    
    //Output everything that's been consumed already in square brackets.
    if(consumePosition > 0)
    {
      //Open square.
      sb.append("[");

      //Write everything behind the consume position.
      for(int i = 0; i < consumePosition; i++)
      {
        //Append ' ', '.' or ',' depending on totBy.
        sb.append(markerChar(i));
        
        //Do the rest.
        sb.append(String.format("%1$02x", reader.thisBuffer.get(i)));
      }

      //Open square.
      sb.append("] ");
    }
    
    //Output everything that has been produced but not yet consumed.
    int i = consumePosition;
    int lim = procPos;
    while(i < lim)
    {
      //Append ' ', '.' or ',' depending on totBy.
      sb.append(markerChar(i));

      try
      {
        sb.append(String.format("%1$02x", reader.thisBuffer.get(i)));        
      }
      catch(Throwable t)
      {
        System.out.println(t);
      }

      //Increment i.
      i++;
    }
    
    //Restore the buffer limit now the whole available buffer has been printed out.
    thisBuffer.limit(savelim);
    
    return sb.toString();
  }
  
  private char markerChar(int i)
  {
    //Append ' ', '.' or ',' depending on totBy.
    if(i == 0)
    {
      return ' ';
    }
    else if( (i % 100) == 0)
    {
      return ',';
    }
    else if( (i % 10) == 0)
    {
      return '.';
    }
    
    return ' ';
  }
  
  /**
   * <p>Switch to PRODUCE mode.
   */
  protected void switchProduce()
  {
    //Only do anything if we're not in PRODUCE mode.
    if(mode != MODE.PRODUCE)
    {
      //Set the position to the current limit (the amount of consumable and consumed data present in the buffer) and the limit to the capacity.
      thisBuffer.position(producePosition);
      thisBuffer.limit(thisBuffer.capacity());
      mode = MODE.PRODUCE;
    }
  }
  
  /**
   * <p>Switch to CONSUME mode.
   */
  protected void switchConsume()
  {
    //Only do anything if we're not in CONSUME mode.
    if(mode != MODE.CONSUME)
    {
      //Set the limit to the current position (which is the position to begin writing from) and the position to the saved
      //consume position.
      thisBuffer.limit(producePosition);
      try
      {
        thisBuffer.position(consumePosition);
      }
      catch(Throwable t)
      {
        throw t;
      }
      mode = MODE.CONSUME;
    }
  }
  
  /**
   * <p>Subclasses can respond to rebuffering events.  This exposes the underlying ByteBuffer instance so this must be used with caution.
   * 
   * @param newBuf the new buffer created as a result of the rebuffering operation.  Subclasses can override this method to provide specific behaviour.
   */
  protected void notifyHandleRebuffer(ByteBuffer newBuf)
  {
    //Dummy method - subclasses override with their own implementation.
  }
  
  /**
   * <p>Allow subclasses to request a rebuffering operation.
   * 
   * @param expansionBytesRequested the minimum number of bytes to expand the buffer by.
   */
  protected void requestRebuffer(int expansionBytesRequested)
  {
    writer.rebuffer(expansionBytesRequested);
  }
  
  /**
   * <p>Return the number of bytes in the writer's current block.  The buffer must be executing a production operation in order to call this method.
   * 
   * @return the number of bytes in the currently produced message block.
   */
  protected int currentProduceBlockBytes()
  {
    //Message MUST be in production to call this method.
    if(produceRecursionLevel < 1) throw new IllegalStateException();
    //Return the block size from the writer.
    return writer.blockMessageBytes();
  }

  /**
   * <p>Return the number of bytes in the writer's current block.  The buffer must be executing a consumption operation in order to call this method.
   * 
   * @return the number of bytes in the currently consumed message block.
   */
  protected int currentConsumeBlockBytes()
  {
    //Message MUST be in consumption to call this method.
    if(consumeRecursionLevel < 1) throw new IllegalStateException();
    //Block size is simply the number of bytes between position and limit.
    return thisBuffer.remaining();
  }
  
  /**
   * <p>Allow subclasses to determine if there is an operation currently in progress.
   * 
   * @return true if there is a reentrant produce or consume operation in progress. 
   */
  protected boolean isOperationLock()
  {
    return operationLock;
  }
  
  /**
   * <p>The buffer was updated by a subclass.  Reflect the change in direct buffer access of the produce position.
   * 
   * @param producePosition the block produce position to reflect the update of.
   */
  protected void reflectConsumableBlockUpdate(int producePosition)
  {
    this.producePosition = producePosition;
    thisBuffer.limit(producePosition);
    
    //Move the buffer back to the consume position.
    thisBuffer.position(consumePosition);
  }
  
  /**
   * <p>The buffer has been updated directly by a subclass.  Update the produce position as a result.
   */
  protected void reflectProducePositionUpdate()
  {
    this.producePosition = thisBuffer.position();
  }
  

  /**
   * <p>The buffer has been updated directly by a subclass.  Update the consume position as a result.
   */
  public void reflectConsumePositionUpdate()
  {
    this.consumePosition = thisBuffer.position();
  }

}
