package hamster.comm.buffer.pipeline;

/**
 * This utility class is the basis for {@link PipelineConsumer} implementations which process
 * delimited-length messages.
 *
 * This class can be used when there is a field within a message which specifies the length
 * of the entire message.  This field is processed and, if there are at least this number of
 * bytes available to be read, then the implementation callback method will be called to deal
 * with the complete message.
 */
public abstract class PipelineBlockConsumer implements PipelineConsumer
{
  public enum LEN_FIELD_TYPE
  {
    i1(1),
    u2(2),
    i2(2),
    i4(4),
    ;

    private final int numBytes;

    private LEN_FIELD_TYPE(int numBytes)
    {
      this.numBytes = numBytes;
    }
  }

  private final int lengthFieldByteOffset;

  private final LEN_FIELD_TYPE lenFieldType;


  private PipelineBlockConsumer()
  {
    lengthFieldByteOffset = 0;
    lenFieldType = LEN_FIELD_TYPE.i2;
  }

  public PipelineBlockConsumer(int lengthFieldByteOffset, LEN_FIELD_TYPE lenFieldType)
  {
    this.lengthFieldByteOffset = lengthFieldByteOffset;
    this.lenFieldType = lenFieldType;
  }

  @Override
  public int consumeFromBuffer(SequentialBlockReader reader)
  {
    //Determine if we can consume a message.
    if(reader.bytesRemainingInBlock() >= lengthFieldByteOffset + lenFieldType.numBytes)
    {
      //Consume the message.
      return handleBlockData(reader);
    }
    else
    {
      //No data consumed.
      return 0;
    }
  }

  protected abstract int handleBlockData(SequentialBlockReader reader);
}
