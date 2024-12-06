package hamster.comm.buffer.pipeline;

/**
 * Pass a callback to a pipeline buffer implementation which is called with a {@link SequentialBlockReader} instance.
 * This instance can be used to consume a block of structured primitive datatypes.
 *
 *
 */
public interface PipelineConsumer
{
  /**
   * Accept a call from a pipeline buffer implementation with a reader object that can be called to consume primitive
   * datatypes.
   *
   * When the consumer is called, it should read an entire block of data.  This is typically a delimited communications
   * message which has a set length, usually specified by a length field within the message.  Communications messages
   * can also be of a fixed length only.
   * The overall number of bytes in the message should be returned in the int return parameter.  Not all message bytes
   * need to be consumed, but by returning the actual message length the pipeline buffer implementation can adjust the
   * consume pointer to the start of the next message, avoiding framing errors.
   * A value of 0 can also be returned if there are consumable data in the buffer but not enough to process the complete
   * message.  In this case, the pipeline buffer will set the consume pointer back to the start of the message, and
   * the whole process can be repeated from the start of the message when there are more consumable data in the buffer.
   *
   * @param reader the reader to call to access readable data.
   * @return the number of bytes in the completely processed block, or 0 if the block can't be processed until more data are available.
   */
  public int consumeFromBuffer(SequentialBlockReader reader);
}
