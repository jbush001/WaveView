import java.util.*;

public class IdentifierValueFormatter implements ValueFormatter
{
	public class Mapping
	{
		Mapping(int _value, String _name)
		{
			value = _value;
			name = _name;
		}
	
		int value;
		String name;
	};

	public IdentifierValueFormatter()
	{
	}

	void addMapping(int value, String name)
	{
		fMappings.add(new Mapping(value, name));
	}
	
	void setNameAtIndex(int index, String name)
	{
		fMappings.elementAt(index).name = name;
	}
	
	void setValueAtIndex(int index, int value)
	{
		fMappings.elementAt(index).value = value;
	}

	int getValueByIndex(int index)
	{
		return fMappings.elementAt(index).value;
	}

	String getNameByIndex(int index)
	{
		return fMappings.elementAt(index).name;
	}

	int getMappingCount()
	{
		return fMappings.size();
	}

	public String format(BitVector bits)
	{
		int mapIndex = bits.intValue();
		for (Mapping m : fMappings)
		{
			if (m.value == mapIndex)
				return m.name;
		}

		// If this doesn't have a mapping, print the raw value
		return "??? (" + Integer.toString(mapIndex) + ")";
	}

	private Vector<Mapping> fMappings = new Vector<Mapping>();
}
