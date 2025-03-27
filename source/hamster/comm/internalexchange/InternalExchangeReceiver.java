package hamster.comm.internalexchange;

/**
 * Callback to receive events.
 *
 * @param <T>
 */
public interface InternalExchangeReceiver<T>
{
  /**
   * Receive the given event.
   *
   * @param t the event data.
   */
  public void receive(T t);
}
