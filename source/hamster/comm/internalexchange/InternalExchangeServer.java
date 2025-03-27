package hamster.comm.internalexchange;

import java.util.HashMap;
import java.util.Map;

/**
 * A <b>VERY SIMPLE</b> message passing framework for passing events between objects within the same thread.
 * This is intended for use within a communications application which is run entirely in one thread.
 *
 * @param <T>
 */
public class InternalExchangeServer<T> implements InternalExchangeSender<T>
{
  //Exchange map.
  private final Map<Integer, InternalExchangeReceiver<T>> exchangeMap = new HashMap<>();

  /**
   * Register the receiver on the given channel id.
   *
   * @param channel channel id for the receiver.  Events sent to that channel id will be routed to the receiver.
   * @param receiver the receiver to receive events for the given channel id.
   */
  public void register(int channel, InternalExchangeReceiver<T> receiver)
  {
    exchangeMap.put(channel, receiver);
  }

  /**
   * Send an event to the given channel id.
   *
   * @param toChannel channel id for the receiver.  Events sent to that channel id will be routed to the receiver.
   * @param t the event instance to send.
   */
  @Override
  public void send(int toChannel, T t)
  {
    //Get the channel receiver.
    InternalExchangeReceiver<T> channelReceiver = exchangeMap.get(toChannel);

    //No channel registered is a runtime exception.  This is a simple implementation and each sender must know what
    //the available channels are.
    if(channelReceiver == null)
    {
      throw new IllegalArgumentException();
    }

    //Send the message to the receiver.
    channelReceiver.receive(t);
  }
}
