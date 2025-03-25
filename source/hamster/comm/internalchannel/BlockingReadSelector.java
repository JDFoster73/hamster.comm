package hamster.comm.internalchannel;

/*
Helper for channels.

Use a selector:
	can wait for timeouts.
	can interrupt the selector in case the thread needs to shut down or for any other reason.

For a standard read-write helper, read non-blocking using the selector then write in blocking?

Read method in a helper:
	Data channel into nonblocking.
	Control channel already in nonblocking.
	Select
		Data channel ready - mark as readable.  Put the channel back into blocking.
		Control channel ready - read control code and do something with it (1 = shutdown?)

	Return.
	…
	Channel is readable, transfer data from it.
	Can write outgoing data to blocking channel after processing read data.
*/

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class BlockingReadSelector
{
  private final Selector selector;

  public BlockingReadSelector() throws IOException
  {
    this.selector = Selector.open();
  }

  public boolean checkReadable(SocketChannel channel, int maxOperationTimeMS) throws IOException
  {
    //Put the channel into non-blocking mode.
    channel.configureBlocking(false);

    SelectionKey key = channel.register(selector, SelectionKey.OP_READ);

    //Select the channel for readability for a maximum of the given time.
    int i = selector.select(maxOperationTimeMS);

    //Clear selections from the selector.
    selector.selectedKeys().clear();
    key.cancel();
    selector.selectNow();  //Clear keys out.

    //Put the channel back into blocking mode.
    channel.configureBlocking(true);

    //Return readable status.
    return i != 0;
  }
}
