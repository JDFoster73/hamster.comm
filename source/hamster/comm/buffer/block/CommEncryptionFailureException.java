package hamster.comm.buffer.block;

/**
 * <p>Communication encryption failure exception.
 * 
 * @author jdf19
 *
 */
@SuppressWarnings("serial")
public class CommEncryptionFailureException extends Exception
{
  /**
   * <p>
   * Raise a comm encryption failure with the given underlying cause.
   * 
   * @param cause the underlying cause for the failure.
   */
  public CommEncryptionFailureException(Throwable cause)
  {
    super(cause);
  }
}
