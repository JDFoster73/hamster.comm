package hamster.comm.itf.listener;

/**
 * <p>This interface adds the concept of a bi-directional graceful close mechanism to an incoming and outgoing and closable data
 * channel.
 * @author jdf19
 *
 */
public interface BiDirectionalChannelListener extends IncomingChannelListener, OutgoingChannelListener
{

}
