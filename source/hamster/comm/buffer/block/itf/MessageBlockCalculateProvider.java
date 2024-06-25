package hamster.comm.buffer.block.itf;

/**
 * <p>Implementing classes allow block calculation on their available content using the {@link ConsumableCalculationBlockHandler} instance
 * provided.  This instance will be called back, allowing the content to be examined. 
 * 
 * @author jdf19
 *
 */
public interface MessageBlockCalculateProvider
{
  /**
   * <p>The implementing instance will provide a mechanism to atomically examine a block of message data.  
   * 
   * @param callback handler for block message data.
   */
  public void doBlockCalculation(ConsumableCalculationBlockHandler callback);
}
