
SOURCES = \
	TraceSettingsFile.java \
	ValueFormatter.java \
	BinaryValueFormatter.java \
	DecimalValueFormatter.java \
	HexadecimalValueFormatter.java\
	IdentifierValueFormatter.java \
	TimescaleView.java \
	MappingView.java \
	NetNameView.java \
	TraceView.java \
	NetSearchView.java \
	WaveformView.java \
	NetTreeModel.java \
	TraceViewModel.java \
	TraceDataModel.java \
	VCDLoader.java \
	WaveApp.java \
	MarkerListView.java \
	Query.java \
	SortedVector.java \
	WaveformPainter.java \
	SingleNetPainter.java \
	MultiNetPainter.java \
	TraceLoader.java \
	AppPreferences.java \
	ColorPreferencePane.java \
	ASCIIValueFormatter.java  \
	TransitionVector.java \
	Transition.java \
	AbstractTransitionIterator.java \
	BitVector.java \
	ColorButton.java \
	PreferenceWindow.java

waveview.jar: $(SOURCES) manifest CLASSDIR
	javac -g $(SOURCES) -d CLASSDIR
	jar cvfm waveview.jar manifest -C CLASSDIR .
	jar uvf	waveview.jar -C resources .

clean:
	rm -rf CLASSDIR
	rm waveview.jar

CLASSDIR: 
	mkdir CLASSDIR
