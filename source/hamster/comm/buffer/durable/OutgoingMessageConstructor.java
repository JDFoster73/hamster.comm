/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.buffer.durable;

import hamster.comm.buffer.block.itf.WriteBlockHandler;

/**
 * <p>Implementations of this interface will provide outgoing message send services.  They accept outgoing data construction
 * providers and send the data requested.
 * 
 * @author jdf19
 */
public interface OutgoingMessageConstructor
{
  /**
   * <p>Construct an outgoing message given the writable block handler.  The handler will be called on to provide the message data
   * to send.
   * 
   * @param blockWriteCallback the object to use to produce the outgoing data with.
   * @return the number of bytes produced.
   */
  void constructOutgoingMessage(WriteBlockHandler blockWriteCallback);
}
