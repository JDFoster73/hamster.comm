package hamster.comm.server.exception;

/**
 * An exception raised in response to attempting to register a channel.
 * 
 * @author jdf19
 *
 */
@SuppressWarnings("serial")
public class ChannelRegistrationException extends Exception
{
  public ChannelRegistrationException()
  {

  }

  public ChannelRegistrationException(Throwable t)
  {
    super(t);
  }
}
