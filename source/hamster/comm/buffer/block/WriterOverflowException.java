package hamster.comm.buffer.block;

/**
 * <p>Overflow exception while writing block data.
 * 
 * @author jdf19
 *
 */
@SuppressWarnings("serial")
public class WriterOverflowException extends RuntimeException
{
  /**
   * <p>The writer that caused the overflow exception.
   */
  private final SequentialMessageBlockWriter cause;
  
  /**
   * <p>Construct exception instance. 
   * 
   * @param cause the cause of the write overflow.
   */
  public WriterOverflowException(SequentialMessageBlockWriter cause)
  {
    this.cause = cause;
  }
  
  /**
   * <p>Get the writer which is the cause of the write overflow exception.
   * 
   * @return the cause.
   */
  public SequentialMessageBlockWriter getBuffer()
  {
    return cause;
  }
}
