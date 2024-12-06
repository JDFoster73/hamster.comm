package hamster.comm.buffer.pipeline;

public abstract class PipelineBlockConsumer implements PipelineConsumer
{
  private enum LEN_FIELD_TYPE
  {
    i1,
    u2,
    i2,
    i4,
    ;
  }

  private final int lengthFieldByteOffset;

  private final LEN_FIELD_TYPE lenFieldType;


  private PipelineBlockConsumer()
  {
    lengthFieldByteOffset = 0;
    lenFieldType = LEN_FIELD_TYPE.i2;
  }

  protected PipelineBlockConsumer(int lengthFieldByteOffset, LEN_FIELD_TYPE lenFieldType)
  {
    this.lengthFieldByteOffset = lengthFieldByteOffset;
    this.lenFieldType = lenFieldType;
  }

  @Override
  public int consumeFromBuffer(SequentialBlockReader reader)
  {
    return 0;
  }

  protected abstract void handleBlockData(SequentialBlockReader reader);
}
