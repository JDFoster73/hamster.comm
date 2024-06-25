package hamster.comm.buffer.block.itf;

/**
 * <p>Implementations hold a defined block of byte data and will allow the caller to consume it by providing a {@link ReadBlockHandler} instance
 * which accepts the data in this block.
 * 
 * @author jdf19
 *
 */
public interface DefinedMessageBlockConsumeProvider
{
  /**
   * <p>This method is called on the provider with a handler instance.  That handler instance is called with the internal buffer
   * reference so that it may consume data from the source.
   * 
   * @param callback handler for block message data.
   */
  public void consumeToHandler(ReadBlockHandler callback);
}
