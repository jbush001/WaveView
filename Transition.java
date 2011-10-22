public class Transition extends BitVector
{
	public long getTimestamp()
	{
		return fTimestamp;
	}

	void setTimestamp(long timestamp)
	{
		fTimestamp = timestamp;
	}
	
	private long fTimestamp;
}
