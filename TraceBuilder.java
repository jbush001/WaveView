// 
// This is a builder pattern that the loader calls into to populate
// information from a trace into the model.
//
interface TraceBuilder
{
	void enterModule(String name);
	void exitModule();
	int newNet(String shortName, int cloneId, int width);
	int getNetWidth(int netId); 	/// @bug This is a hack.  Clean up.
	void appendTransition(int id, long timestamp, BitVector values);
	void loadFinished();
}