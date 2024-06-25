package hamster.comm.buffer.block.itf;

import java.nio.channels.ReadableByteChannel;

/**
 * <p>Classes implementing this interface provide fill block production services.
 * This allows a <i>fill block</i> to be defined.  A fill block is a defined block of data
 * which will be filled from a channel.  If there are more data in the implementor than are
 * specified in the fill block then they will not be filled using methods defined in this
 * interface; only the data <b>specifically defined</b> in the fill block will be filled.
 * 
 * @author jdf19
 *
 */
public interface FillBlockProvider
{
  /**
   * <p>Read fill block data from the given channel.  If there is no fill block in progress,
   * one will be created.  <b>Care must be taken</b> when calling this method.  If the previous
   * call completed the fill block then the next one will be created upon the next call of this method.  
   * If other actions need to be taken before receiving the next fill block then following this call with
   * {@link FillBlockProvider#bytesRemainingInDrainBlock()} to check if there are more bytes
   * to send is <b>essential</b>.
   * 
   * @param channel the channel to read fill block data from.  No data outside the fill block will be read.
   * @return the number of bytes read from the channel.
   */
  public int fillBlockFromChannel(ReadableByteChannel channel);
  
  /**
   * <p>The number of bytes remaining in the current fill block.  This will throw a runtime exception
   * if called when a fill block has not been started with a prior call to {@link FillBlockProvider#fillBlockFromChannel(ReadableByteChannel)}
   * command.
   * <p>If this returns zero, then the next call to {@link FillBlockProvider#fillBlockFromChannel(ReadableByteChannel)} will start a new fill block.
   * @return the number of bytes left to receive in the currently defined fill block.
   */
  public int bytesRemainingInDrainBlock();
}
