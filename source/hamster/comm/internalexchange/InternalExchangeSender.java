package hamster.comm.internalexchange;

/**
 * Interface to send events.
 *
 * @param <T>
 */
public interface InternalExchangeSender<T>
{
  /**
   * Send the event to the given channel id.
   *
   * @param toChannel the channel id to send the event to.
   * @param t the event to send.
   */
  public void send(int toChannel, T t);
}
