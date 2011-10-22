
public class BinaryValueFormatter implements ValueFormatter
{
	public String format(BitVector bits)
	{
		return bits.toString(2);
	}
}
