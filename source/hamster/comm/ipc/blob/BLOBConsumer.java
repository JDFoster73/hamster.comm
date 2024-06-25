package hamster.comm.ipc.blob;

import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.buffer.block.itf.WholeBufferConsumeProvider;

/**
 * <p>The instance backing this interface allows BLOB data to be consumed.  Data can be consumed in blocks using the standard methods from the
 * {@link DrainableChannelBuffer} and {@link WholeBufferConsumeProvider} interfaces.
 * The length of the BLOB in bytes can be ascertained by calling the {@link #getBLOBLen()} method.
 * <p>When the caller has finished accessing the BLOB and no longer needs it, it <b>must</b> call {@link #finish()} in order to release the BLOB resources.
 * Failing to do this will cause a memory leak as the BLOB object will be held perpetually until the end of the process.
 * <p>The {@link #setAllConsumable()} method can be called to set the whole BLOB data consumable.  This is useful if the BLOB data
 * need to be consumed multiple times.
 * 
 * @author jdf19
 *
 */
public interface BLOBConsumer extends DrainableChannelBuffer, WholeBufferConsumeProvider
{
  /**
   * <p>Get the number of accessible bytes in the BLOB.
   * 
   * @return the number of accessible bytes in the BLOB.
   */
  public int getBLOBLen();
  
  /**
   * <p>It is <b>imperative</b> that this method be called when the BLOB is finished with.  The BLOB may need to be accessed over multiple calls
   * so it cannot be automatically disposed of.  This being the case, if this {@link #finish()} method is not called then a memory leak will
   * ensue.
   */
  public void finish();

  /**
   * <p>Set all BLOB data consumable.
   */
  public void setAllConsumable();
}
