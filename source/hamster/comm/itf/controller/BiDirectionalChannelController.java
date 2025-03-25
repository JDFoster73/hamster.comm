package hamster.comm.itf.controller;

/**
 * <p>Base bi-directional channel controller interface.  It simply amalgamates the base, read and write controller interfaces and provides no
 * implementation methods of its own.
 *  
 * @author jdf19
 *
 */
public interface BiDirectionalChannelController extends BaseChannelController, ReadableChannelController, WritableChannelController
{  
  
}
