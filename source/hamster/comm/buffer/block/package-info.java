/**
 * <p>Buffering support for buffers which produce and/or consume messages as whole blocks of data.  A <b><i>block</i></b> of data is a complete, self-contained message that is understood in the same way at both the
 * producer and the consumer end.  These block pipeline buffer classes build on the java.nio ByteBuffer and aim to simplify
 * its usage.  Producing whole blocks of data can be inconvenient in one way, because the user must supply a {@link hamster.comm.buffer.block.itf.WriteBlockHandler} callback
 * to produce a message block's worth of data in one operation.  A consume operation must also provide a {@link hamster.comm.buffer.block.itf.ReadBlockHandler} or a {@link hamster.comm.buffer.block.itf.ReadAllHandler}
 * callback to consume a block of data.  The advantage of using blocks in this way is that writing or reading a block of data will be done in a single call.  Consuming a block of
 * message data in one go can be helpful in that message framing is automatic.  If any data at the end of a message are not consumed while handling the block (for example)
 * then that data can be silently discarded and the buffer consume position set automatically to the start of the next message block. 
 * <p>
 * When producing a message, programming is (arguably) simplified because the entire block will be produced in one call.
 * <p>
 * Channels can <b><i>fill</i></b> buffers with data.  <b><i>Filling</i></b> is the process of taking data from a channel and filling the buffer with it.  The resulting data
 * may or may not contain complete message blocks.  The presence of complete message blocks is determined during consumption.
 * <p>
 * Buffers can <b><i>drain</i></b> to a channel.  Drained data will leave the buffer and be written to the channel. 
 * <p>
 * Message blocks can be <b><i>produced</i></b> to a buffer.  Messages blocks are produced atomically.  These blocks are then available for <b><i>consuming</i></b> or <b><i>draining</i></b>.
 * <p>
 * Message blocks can be <b><i>consumed</i></b> from a buffer.  Messages blocks are consumed atomically.   
 * <p>
 * The general usage of block buffers is: <br>
 * <ul>
 * <li>PRODUCE>CONSUME>PRODUCE>... : internal buffers (ones not related to physically sending and receiving data over a channel) use this pattern.  Message block data must be produced before it can be consumed.
 * <li>FILL>CONSUME>FILL>... : network buffers which receive data from a channel use this pattern.  Incoming block message data is consumed and actioned.
 * <li>PRODUCE>DRAIN>PRODUCE>... : network buffers which send data to a channel use this pattern.  Outgoing block message data is produced and sent (drained) to a channel.
 * </ul> 
 */

package hamster.comm.buffer.block;