package hamster.comm.buffer.block.itf;

import java.nio.channels.WritableByteChannel;

/**
 * <p>Classes implementing this interface provide drain block consumption services.
 * This allows a <i>drain block</i> to be defined.  A drain block is a defined block of data
 * which will be drained to a channel.  If there are more data in the implementor than are
 * specified in the drain block then they will not be drained using methods defined in this
 * interface; only the data <b>specifically defined</b> in the drain block will be drained.
 * 
 * @author jdf19
 *
 */
public interface DrainBlockProvider
{
  /**
   * <p>Write drain block data to the given channel.  If there is no drain block in progress,
   * one will be created.  <b>Care must be taken</b> when calling this method.  If the previous
   * call completed the drain block then the next one will be created.  If other actions
   * need to be taken before sending the next drain block then following this call with
   * {@link DrainBlockProvider#bytesRemainingInDrainBlock()} to check if there are more bytes
   * to send is <b>essential</b>.
   * 
   * @param channel the channel to write drain block data to.  No data outside the drain block will be written.
   * @return the number of bytes written to the channel.
   */
  public int drainBlockToChannel(WritableByteChannel channel);
  
  /**
   * <p>The number of bytes remaining in the current drain block.  This will throw a runtime exception
   * if called when a drain block has not been started with a prior call to {@link DrainBlockProvider#drainBlockToChannel(WritableByteChannel)}
   * command.
   * <p>If this returns zero, then the next call to {@link DrainBlockProvider#drainBlockToChannel(WritableByteChannel)} will start a new drain block.
   * @return the number of bytes left to send in the currently defined drain block.
   */
  public int bytesRemainingInDrainBlock();
  
  /**
   * <p>If data need to be resent then this method can be used to locate the drain block send pointer to the correct position.  This will throw a runtime exception
   * if called when a drain block has not been started with a prior call to {@link DrainBlockProvider#drainBlockToChannel(WritableByteChannel)}
   * command.
   * 
   * @param blockByteSendPosition the byte offset within the currently defined drain block.
   */
  public void locateDrainBlockSendPosition(int blockByteSendPosition);
  
  /**
   * <p>Acknowledge receipt of a given number of bytes.  These bytes can be discarded from the underlying storage.
   * 
   * @param bytesAcknowledged the number of bytes acknowledged as having been received.
   */
  public void acknowledgeReceipt(int bytesAcknowledged);
}
