package test.hamster.emul.netchannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.slf4j.Logger;


public class TestChannelBufferBridge implements ReadableByteChannel, WritableByteChannel
{
  /**
   * <p>Channel data are transferred into the receive buffer from a producer, and transferred to the send buffer on advance() calls.
   */
  private final ByteBuffer fillBuffer;

  /**
   * <p>Channel data are transferred into the send buffer from the receive buffer on advance() calls, and read by a consumer.
   */
  private final ByteBuffer drainBuffer;

  /**
   * <p>Count the number of advance() calls without transfer.  This is used to make sure data are cleared out after so many advance() calls.
   */
  private int noTransferOnAdvanceCounter = 0;
 
  /**
   * <p>The transfer decision tells the bridge how many bytes to transfer from recieve to send buffer.
   */
  private final TransferDecision transferDecision;
  
  /**
   * <p>Logger for emitting test messages. 
   */
  private final Logger logger;
  
  public TestChannelBufferBridge(ByteBuffer receiveBuffer, ByteBuffer sendBuffer, TransferDecision transferDecision, Logger logger)
  {
    this.fillBuffer = receiveBuffer;
    this.drainBuffer = sendBuffer;
    
    this.transferDecision = transferDecision;
    
    this.logger = logger;
    
    init();
  }

  private final void init()
  {
    //Clear both buffers.
    fillBuffer.clear();
    drainBuffer.clear();
    
    //Make sure drain buffer is flipped to start.
    drainBuffer.flip();    
  }
  
  /**
   * <p>Always open.
   */
  @Override
  public boolean isOpen()
  {
    return true;
  }

  /**
   * <p>Can't close this.
   */
  @Override
  public void close() throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * <p>Write into the bridge receive buffer.  This will be in a permanent writable state, compact and recover when draining.
   */
  @Override
  public int write(ByteBuffer src) throws IOException
  { 
    //Write from source buffer to receive buffer.
    int recvPos = fillBuffer.position();
    
    //Work out bytes to transfer.  Can't transfer more to the receive buffer than it has space for.
    int bytesToTransfer = Math.min(fillBuffer.remaining(), src.remaining());
    
    //Don't do anything if there's no bytes to transfer.
    if(bytesToTransfer == 0) return 0;

    //Adjust the limit of the src to reflect the number of bytes to transfer.
    int srcLimit = src.limit();
    src.limit(src.position() + bytesToTransfer);
    
    //Transfer the data.
    fillBuffer.put(src);
    
    //Restore the limit on the source buffer.
    src.limit(srcLimit);
    
    //Return number of bytes written in.
    return fillBuffer.position() - recvPos;
  }

  /**
   * <p>Read from the bridge receive buffer.
   */
  @Override
  public int read(ByteBuffer dst) throws IOException
  {
    //Transfer from write buffer to dst buffer.
    int pos = drainBuffer.position();

    //Work out bytes to transfer.  Can't transfer more to the receive buffer than it has space for.
    int bytesToTransfer = Math.min(drainBuffer.remaining(), dst.remaining());
    
    //Don't do anything if there's no bytes to transfer.
    if(bytesToTransfer == 0) return 0;
    
    //Adjust the limit of the src to reflect the number of bytes to transfer.
    int srcLimit = dst.limit();
    dst.limit(dst.position() + bytesToTransfer);
    
    //Transfer the data.
    int dstLimit = drainBuffer.limit();
    drainBuffer.limit(drainBuffer.position() + bytesToTransfer);
    dst.put(drainBuffer);
    
    //Restore the limit on the dest and send buffer.
    dst.limit(srcLimit);
    drainBuffer.limit(dstLimit);
    
    // TODO Auto-generated method stub
    return drainBuffer.position() - pos;
  }

  /**
   * <p>Transfer data from send to receive buffer.
   */
  void advance()
  {
    //Prepare FILL buffer for transfer.  It is in write mode; pos = write pos, lim = capacity.  We need to flip() so that pos = 0 and lim = write pos.
    fillBuffer.flip();
    
    //Prepare DRAIN buffer for transfer.  It is in read mode; pos = read pos, lim = readable limit.  We need to compact() so that pos = readable limit and lim = capacity, recovering any consumed data.
    drainBuffer.compact();
    
    //Get the transfer decision first.
    int bytesToTransfer = transferDecision.bytesToTransfer(noTransferOnAdvanceCounter, fillBuffer.remaining(), drainBuffer.remaining());

    //Calculate the no transfer counter.
    if(bytesToTransfer == 0)
    {
      noTransferOnAdvanceCounter++;
      //Compact the fill buffer and compact the drain buffer.
      fillBuffer.compact();
      drainBuffer.flip();
      return;
    }
    
    //Set the fill buffer limit to reflect the number of bytes to transfer.
    int fillLim = fillBuffer.limit();
    fillBuffer.limit(fillBuffer.position() + bytesToTransfer);
    
    //Do the transfer.
    drainBuffer.put(fillBuffer);
    
    //Put the limit back to the fill buffer.
    fillBuffer.limit(fillLim);
    
    //Compact the fill buffer and compact the drain buffer.
    fillBuffer.compact();
    drainBuffer.flip();
  }

  public boolean has()
  {
    return drainBuffer.hasRemaining() || fillBuffer.position() > 0;
  }

  @Override
  public String toString()
  {
    return "[FL:" + fillBuffer.position() + "|DR:" + drainBuffer.remaining() + "]";
  }

  /**
   * <p>Reset the buffer - clear out all data and put back to start.
   */
  public void reset()
  {
    init();
  }

  public int bytesOfTransferData()
  {
    int txBytes = fillBuffer.position() + drainBuffer.remaining();
    return txBytes;
  }
  
  
}

////Flip the receive buffer to prepare for transfer.
//fillBuffer.flip();
//
////Receive buffer is flip()ped.  If remaining is 0, there's no data to transfer.
//if(!fillBuffer.hasRemaining())
//{
//fillBuffer.compact();
//return;
//}
//
////Receive buffer flipped for writing.  Compact send buffer for writing.
//drainBuffer.compact();
//
////Get the number of bytes to transfer.
//int bytesToTransfer = transferDecision.bytesToTransfer(noTransferOnAdvanceCounter, fillBuffer.remaining(), drainBuffer.remaining());
//
////Calculate the no transfer counter.
//if(bytesToTransfer == 0)
//{
//noTransferOnAdvanceCounter++;
//return;
//}
//
////Got bytes to transfer.
//noTransferOnAdvanceCounter = 0;
//
////Set the limit to the number of bytes to transfer.
//drainBuffer.limit(Math.min(drainBuffer.capacity(), drainBuffer.position() + bytesToTransfer));
//
////Make sure the receive buffer will not overflow the send buffer upon transfer.
//int rbufLim = fillBuffer.limit();
//fillBuffer.limit(Math.min(rbufLim, drainBuffer.limit()));
////Transfer the data.
//drainBuffer.put(fillBuffer);
//
////Restore the original limit to the receive buffer.
//fillBuffer.limit(rbufLim);
//
////Set the limit of the send buffer back to capacity.
//drainBuffer.limit(drainBuffer.capacity());
//
////Finished transferring - compact() the receive buffer.
//fillBuffer.compact();
//
////Flip() the send buffer.
//drainBuffer.flip();
