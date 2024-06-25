package hamster.comm.buffer.block;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import org.slf4j.Logger;

import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.buffer.FillableChannelBuffer;
import hamster.comm.buffer.block.itf.MessageBlockConsumeProvider;
import hamster.comm.buffer.block.itf.MessageBlockProduceProvider;
import hamster.comm.buffer.block.itf.ReadAllHandler;
import hamster.comm.buffer.block.itf.ReadBlockHandler;
import hamster.comm.buffer.block.itf.ReadableCompletionBlockHandler;
import hamster.comm.buffer.block.itf.WholeBufferConsumeProvider;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.buffer.factory.GrowUpdateExpandableBufferFactory;
import hamster.comm.logging.DummyLogger;

/**
 * <p>This class adds cryptography services to the standard Hamster buffer mechanism.  It makes use of the reentrant properties of
 * produce and consume operations to allow for the encryption and decryption of sections of message data.  It also supports the generation
 * and validation of message digests to help ensure message integrity.
 * 
 * @author jdf19
 *
 */
public class CommEncryptionPipelineBuffer implements MessageBlockProduceProvider, MessageBlockConsumeProvider, WholeBufferConsumeProvider, FillableChannelBuffer, DrainableChannelBuffer
{
  private final CipherEnabledBlockBuffer internalBuf;
  
  /**
   * <p>The last message digest operation was a success.
   */
  private boolean lastDigestSuccess = false;
  
  /**
   * <p>Construct the buffer with the given parameters.
   * 
   * @param bufferFact the factory to use when constructing the buffer
   * @param bufferId buffer identifier to use in logging
   * @param logger logger to use when logging debug statements
   */
  public CommEncryptionPipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
  {
    internalBuf = new CipherEnabledBlockBuffer(bufferFact, bufferId, logger);
  }

  /**
   * <p>Construct the buffer with the given parameters.
   * 
   * @param bufferFact the factory to use when constructing the buffer
   */
  public CommEncryptionPipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact)
  {
    internalBuf = new CipherEnabledBlockBuffer(bufferFact, "", new DummyLogger());
  }

  /**
   * <p>Receive datagram into the buffer.
   * 
   * @param channel the datagram channel to receive data from.
   * @return the address of the datagram sender.
   * @throws IOException if an underlying communication exception was thrown as a result of the transfer.
   */
  public SocketAddress doDatagramReceive(DatagramChannel channel) throws IOException
  {
    return this.internalBuf.doDatagramReceive(channel);
  }

  /**
   * <p>Send encrypted data as a datagram to the given channel.
   * 
   * @param channel the datagram channel to send encrypted buffer data to.
   * @param address the address to send the resulting datagram to.
   * @throws IOException if an underlying communication exception was thrown as a result of the transfer.
   */
  public void doDatagramSend(DatagramChannel channel, SocketAddress address) throws IOException
  {
    this.internalBuf.doDatagramSend(channel, address);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void consumeStartAllBufferData(ReadAllHandler callback)
  {
    internalBuf.consumeAllData(callback);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void consumeNextBufferData(ReadAllHandler callback)
  {
    internalBuf.consumeNextData(callback);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int produceFromHandler(WriteBlockHandler writeProc)
  {
    return internalBuf.produceData(writeProc);    
  }

  @Override
  public boolean consumeToHandler(ReadableCompletionBlockHandler callback)
  {
    return internalBuf.consumeData(callback);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int fromChannel(ReadableByteChannel channel) throws IOException
  {
    //return super.fillData(channel);
    return internalBuf.fillData(channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int fromChannel(ReadableByteChannel channel, int maxBytes) throws IOException
  {
    //return super.fillData(channel);
    return internalBuf.fillData(channel, maxBytes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int drainBufferToChannel(WritableByteChannel channel) throws IOException
  {
    return internalBuf.drainData(channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int drainBufferToChannel(WritableByteChannel channel, int maxBytesToSend) throws IOException
  {
    return internalBuf.drainData(channel, maxBytesToSend);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAcceptTransfer()
  {
    return internalBuf.canAcceptTransfer();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasSpaceFor(int requiredBufferLen)
  {
    return internalBuf.hasSpaceFor(requiredBufferLen);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasDataToConsume()
  {
    //return super.hasDataToConsume();
    return internalBuf.hasDataToConsume();
  }

  /**
   * {@inheritDoc}
   */
  public void clearAll()
  {
    internalBuf.clearAll();
  }
  
  /**
   * {@inheritDoc}
   */
  public int readableBytesInBuffer()
  {
    return internalBuf.consumableBytesInBuffer();
  }

  /**
   * <p>Executes an encryption doFinal(...) operation on the last <code>bytes</code> bytes in the current produce block.
   * This reentrant method <b>must</b> only be used after a {@link #produceFromHandler(WriteBlockHandler)} call and before
   * that call has ended.  The way to do this is to call it from inside a {@link WriteBlockHandler} instance on the same
   * instance of {@link CommEncryptionPipelineBuffer} that the call {@link #produceFromHandler(WriteBlockHandler)} was made on.
   * @param c the cipher to use.
   * @param bytes the number of bytes to operate on.
   */
  public void doFinalEncryptForPrevious(Cipher c, int bytes)
  {
    //Set buffers up.
    internalBuf.doFinalEncryptForPrevious(c, bytes);
  }
  
  /**
   * <p>Executes an decryption doFinal(...) operation on the last <code>len</code> bytes in the current consume block.
   * This reentrant method <b>must</b> only be used after a {@link #consumeToHandler(ReadableCompletionBlockHandler)} call and before
   * that call has ended.  The way to do this is to call it from inside a {@link ReadableCompletionBlockHandler} instance on the same
   * instance of {@link CommEncryptionPipelineBuffer} that the call {@link #consumeToHandler(ReadableCompletionBlockHandler)} was made on.
   * @param c the Cipher to use.
   * @param len the number of bytes to operate on.
   * @return the number of bytes processed as a result of the operation.  
   */
  public int doFinalDecryptForNext(Cipher c, int len)
  {
    return internalBuf.doFinalDecryptForNext(c, len);
  }

  /**
   * <p>Replace the number of consumable bytes of encrypted data with a (possibly smaller) block of decrypted
   * bytes using the given cipher.
   * 
   * @param c the Cipher to use.
   * @param consumableBlockLen the number of encrypted bytes to decrypt.
   * @return the number of bytes resulting.
   */
  public int doCryptConsumableBlockUpdate(Cipher c, int consumableBlockLen)
  {
     return internalBuf.decryptConsumableBlockUpdate(c, consumableBlockLen);
  }
  
  /**
   * <p>Replace the number of consumable bytes of encrypted data with a (possibly smaller) block of decrypted
   * bytes using the given cipher.
   * 
   * @param c the Cipher to use.
   * @param consumableBlockLen the conssumable block length.
   * @return the number of bytes processed as a result of the operation.  
   */
  public int doCryptConsumableBlockFinal(Cipher c, int consumableBlockLen)
  {
    return internalBuf.decryptConsumableBlockFinal(c, consumableBlockLen);
  }
  
  /**
   * <p>Create a message digest for the last number of bytes <b>while producing a message</b>.
   * Within the {@link WriteBlockHandler} execution, this method can be called at any point
   * therein to produce a message digest on the given number of bytes.
   * 
   * @param md the message digest to use.
   * @param bytesToDigest the number of bytes to digest.
   */
  public void doProduceMessageDigest(MessageDigest md, int bytesToDigest)
  {
    internalBuf.doProduceMessageDigest(md, bytesToDigest);
  }
  
  /**
   * <p>Check a message digest on the given number of bytes <b>while consuming a message</b>.
   * Within the {@link ReadBlockHandler#readMessageBlock(SequentialMessageBlockReader)} method
   * implementation, this method can be called to check the message digest on the given
   * number of bytes.  The digest will be produced locally and checked against the message
   * digest bytes immediately after the requested block.
   * 
   * @param md the message digest to use.
   * @param bytesToCheck the number of digest bytes to check.
   */
  public void doCheckMessageDigest(MessageDigest md, int bytesToCheck)
  {
    internalBuf.doCheckMessageDigest(md, bytesToCheck);
  }

  /**
   * <p>Return true if the last message digest check was successful. 
   * 
   * @return success of last message digest check.
   */
  public boolean wasLastMessageDigestCheckSuccess()
  {
    return lastDigestSuccess;
  }
  
  /**
   * <p>This class extends {@link BlockBufferBase} to add cryptographical facilities.
   * @author jdf19
   *
   */
  private class CipherEnabledBlockBuffer extends BlockBufferBase
  {
    /**
     * A duplicate of the underlying buffer.
     */
    private ByteBuffer encryptedBuffer;

    public CipherEnabledBlockBuffer(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
    {
      super(bufferFact, bufferId, logger);
    }

    
    private SocketAddress doDatagramReceive(DatagramChannel channel) throws IOException
    {
      //Switch to produce mode.
      switchProduce();

      //Fill operation with datagram channel support.
      SocketAddress receiveAddress = channel.receive(thisBuffer);
      
      //Update the produce position.
      reflectProducePositionUpdate();
      
      //Return the socket address return value from the operation.
      return receiveAddress;
    }


    private void doDatagramSend(DatagramChannel channel, SocketAddress addr) throws IOException
    {
      //Switch to produce mode.
      switchConsume();
      
      //Send the request.
      channel.send(thisBuffer, addr);
      
      //Update the consume position.
      reflectConsumePositionUpdate();
    }

    /**
     * <p>Notification of rebuffering operation by superclass.  In this implementation we need to duplicate the buffer
     * to provide a target for encryption/decryption operations which has the same underlying storage as the internal
     * buffer.
     */
    @Override
    protected void notifyHandleRebuffer(ByteBuffer newBuf)
    {
      encryptedBuffer = newBuf.duplicate();
    }

    private void doFinalEncryptForPrevious(Cipher c, int bytes)// throws CommEncryptionFailureException
    {
      //Check there are enough bytes in the block to process the request.
      if(internalBuf.currentProduceBlockBytes() < bytes) throw new BufferUnderflowException();
      
      //Set buffers up.
      //
      //Set the limit to the current position.
      thisBuffer.limit(thisBuffer.position());
      //Wind back the position of the internal buffer
      thisBuffer.position(thisBuffer.position() - bytes);
      //Copy state to the encryption buffer.
      encryptedBuffer.limit(encryptedBuffer.capacity());
      encryptedBuffer.position(thisBuffer.position());
      
      //Do the encryption.
      try
      {
        //Check we've got space to do the op.
        int reqdSpace = c.getOutputSize(bytes);
        if(!hasSpaceFor(reqdSpace)) requestRebuffer(bytes);

        //Do the encryption
        c.doFinal(thisBuffer, encryptedBuffer);

        //Update the internal buffer to reflect the operation.
        //
        //thisBuffer - pos:lim, lim:<unchanged>.
        //encryptBuffer - pos:<pos after generating encrypted data>, lim:capacity.
        thisBuffer.limit(thisBuffer.capacity());
        thisBuffer.position(encryptedBuffer.position());
      }
      catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e)
      {
        //These should have been set up correctly!  I argue that these are runtime exceptions and so
        //will be propagated as them.
        throw new RuntimeException(e);
      }

    }    

    private int doFinalDecryptForNext(Cipher c, int bytes)
    {
      //Confirm there are enough bytes to consume.
      if(internalBuf.currentConsumeBlockBytes() < bytes) throw new BufferUnderflowException();
     
      //Set buffers up.
      //
      //Store the buffer limit so we can reinstate it later.
      int bufLimit = thisBuffer.limit();
      int bufPos = thisBuffer.position();
      
      //Set the limit to the current position plus the number of bytes in the encrypted block.
      thisBuffer.limit(thisBuffer.position() + bytes);
      //Copy state to the encryption buffer.
      encryptedBuffer.limit(encryptedBuffer.capacity());
      encryptedBuffer.position(thisBuffer.position());
      
      //Do the encryption.
      try
      {
        //Do the encryption
        int encryptedBlockSizeBytes = c.doFinal(thisBuffer, encryptedBuffer);

        //Update the internal buffer to reflect the operation.
        //
        //thisBuffer - pos:lim, lim:<unchanged>.
        //encryptBuffer - pos:<pos after generating encrypted data>, lim:capacity.
        thisBuffer.limit(bufLimit);
        thisBuffer.position(bufPos);

        //Return the size of the encrypted block.
        return encryptedBlockSizeBytes;
      }
      catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e)
      {
        //These should have been set up correctly!  I argue that these are runtime exceptions and so
        //will be propagated as them.
        throw new RuntimeException(e);
      }
    }
    
    /**
     * <p>Replace the number of consumable bytes of encrypted data with a (possibly smaller) block of decrypted
     * bytes using the given cipher.  This will use the doUpdate(...) cipher operation.
     * 
     * @param cpe
     * @param consumableBlockLen
     */
    private int decryptConsumableBlockUpdate(Cipher c, int consumableBlockLen)
    {
      //Prepare the operation.
       prepare(c.getOutputSize(consumableBlockLen), consumableBlockLen);//int startPos = 
      
      //Do the decryption update.
      try
      {
        //Do the update.
        int blockSize = c.update(thisBuffer, encryptedBuffer);
        
        //Move the params from encrypted buffer to this buffer.
        //
        //
        //Set the new limit
        reflectConsumableBlockUpdate(encryptedBuffer.position());
        
        return blockSize;
      }
      catch (ShortBufferException e)
      {
        //These should have been set up correctly!  I argue that these are runtime exceptions and so
        //will be propagated as them.
        throw new RuntimeException(e);
      }
    }
    
    /**
     * <p>Replace the number of consumable bytes of encrypted data with a (possibly smaller) block of decrypted
     * bytes using the given cipher.  This will use the doFinal(...) cipher operation.
     * 
     * @param cpe
     * @param consumableBlockLen
     */
    private int decryptConsumableBlockFinal(Cipher c, int consumableBlockLen)
    {
      //Prepare the operation.
      int startPos = prepare(c.getOutputSize(consumableBlockLen), consumableBlockLen);

      //Do the decryption update.
      try
      {
        //Do the update.
        int blockSize = c.doFinal(thisBuffer, encryptedBuffer);
        
        //Move the params from encrypted buffer to this buffer.
        //
        //
        thisBuffer.position(startPos);
        thisBuffer.limit(encryptedBuffer.position());
        //Set the new limit
        reflectConsumableBlockUpdate(encryptedBuffer.position());
        
        return blockSize;
      }
      catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e)
      {
        //These should have been set up correctly!  I argue that these are runtime exceptions and so
        //will be propagated as them.
        throw new RuntimeException(e);
      }
    }
    
    /**
     * <p>Prepare the internal buffers for the cryptographic operation.
     * 
     * @param c
     * @param consumableBlockLen
     * @return
     */
    private int prepare(int requiredOutputSize, int consumableBlockLen)//Cipher c, 
    {
      //Check no op is in progress.
      if(isOperationLock()) throw new IllegalStateException();

      //Check we have enough bytes to operate on.
      if(consumableBytesInBuffer() < consumableBlockLen) throw new IllegalArgumentException();
      
      //Make sure we've got enough space in the buffer.
      //int reqdBuf = c.getOutputSize(consumableBlockLen);
      if(!hasSpaceFor(requiredOutputSize)) requestRebuffer(requiredOutputSize);

      //Make sure we're in consume mode.
      switchConsume();
      
      //Set up the buffers.
      //
      //
      thisBuffer.position(thisBuffer.limit() - consumableBlockLen);
      encryptedBuffer.position(thisBuffer.position());
      encryptedBuffer.limit(encryptedBuffer.capacity());

      //Ready to do the op...
      return thisBuffer.position();
    }
    
    /**
     * <p>This operation takes the number of bytes and creates a message digest which is then added
     * to the end of the message, increasing the produce position.  This operation can be called
     * while producing a block message so that the overall length can be calculated after finishing
     * producing the block and digest.
     * 
     * @param digest
     */
    private void doProduceMessageDigest(MessageDigest digest, int bytes)
    {
      //Check there are enough produced bytes in the current block to work on.
      if(currentProduceBlockBytes() < bytes) throw new IllegalArgumentException();
      
      //Check we've got extra space to write the message digest into.
      if(producableBytesInBuffer() < digest.getDigestLength()) requestRebuffer(thisBuffer.limit() + digest.getDigestLength());
      
      //Prepare the buffer for digesting.
      //int lim = thisBuffer.limit();
      int pos = thisBuffer.position();
      thisBuffer.position(thisBuffer.position() - bytes);
      thisBuffer.limit(pos);
      
      //Buffer has been set up.  Do the message digest.
      digest.update(thisBuffer);
      
      //Put the buffer back to the way it was.
      thisBuffer.limit(thisBuffer.capacity());
      thisBuffer.position(pos);
      
      //Add the digest.
      thisBuffer.put(digest.digest());
    }
    
    /**
     * <p>This operation takes the number of bytes from the consume position in the currently consumed block 
     * and creates a message digest which is then checked against the digest bytes immediately after the block.
     * This operation must be called while consuming a block message.
     * @param digest
     */
    private void doCheckMessageDigest(MessageDigest md, int bytesToCheck)
    {
      //Check there are enough produced bytes in the current block to work on.
      if(currentConsumeBlockBytes() < (bytesToCheck + md.getDigestLength())) throw new IllegalArgumentException();
      
      //Set limits on the buffer to calculate the digest.
      int pos = thisBuffer.position();
      int lim = thisBuffer.limit();
      
      //Set up the limit.w
      thisBuffer.limit(thisBuffer.position() + bytesToCheck);
      
      //Do the digeest.
      md.update(thisBuffer);
      
      //Get the digest.
      byte digest[] = md.digest();
      
      //Produced the digest.  Position is at the end of the check block, equal to limit.  Set the
      //limit back to the end of the digest and compare.
      thisBuffer.limit(pos + bytesToCheck + md.getDigestLength());
            
      //
      //Check the digests.
      //Using a simple byte-by-byte test here.  Can't see a convenient way of
      //comparing byteBuffer contents to a byte array other than that.
      if(thisBuffer.remaining() != digest.length) throw new IllegalStateException();

      //Do compare.
      lastDigestSuccess = true;
      int ix = 0;
      while(thisBuffer.hasRemaining())
      {
        if(thisBuffer.get() != digest[ix++])
        {
          lastDigestSuccess = false;
          break;
        }
      }
      
      //Set the buffer pos and lim back to pre-check positions.
      thisBuffer.limit(lim);
      thisBuffer.position(pos);
        
    }
  }
}
