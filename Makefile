# 
# Copyright 2011-2012 Jeff Bush
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 


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
	PreferenceWindow.java \
	QueryWindow.java

waveview.jar: $(SOURCES) manifest CLASSDIR
	javac -g $(SOURCES) -d CLASSDIR
	jar cvfm waveview.jar manifest -C CLASSDIR .
	jar uvf	waveview.jar -C resources .

clean:
	rm -rf CLASSDIR
	rm waveview.jar

CLASSDIR: 
	mkdir CLASSDIR
