//
// Copyright 2016 Jeff Bush
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import waveview.BitVector;
import waveview.WaveformBuilder;

//// Implements WaveformBuilder interface, which the VCDLoader will write into,
/// but asserts if events don't match a pre-defined sequence.
class ExpectWaveformBuilder implements WaveformBuilder {
    static final int EXPECT_ENTER = 0;
    static final int EXPECT_EXIT = 1;
    static final int EXPECT_NET = 2;
    static final int EXPECT_TRANSITION = 3;
    static final int EXPECT_FINISHED = 4;
    static final int EXPECT_TIMESCALE = 5;

    private final ArrayList<Event> eventList = new ArrayList<>();
    private int currentEventIndex;
    private int nextNetId;

    static class Event {
        final int type;
        String name;
        int id;
        int width;
        long timestamp;
        BitVector values;

        Event(int type) {
            this.type = type;
        }
    }

    @Override
    public void setTimescale(int order) {
        System.out.println("setTimescale " + order);

        Event event = eventList.get(currentEventIndex++);
        assertEquals(event.type, EXPECT_TIMESCALE);
        assertEquals(event.timestamp, (long) order);
    }

    @Override
    public void enterScope(String name) {
        System.out.println("enterScope " + name);

        Event event = eventList.get(currentEventIndex++);
        assertEquals(event.type, EXPECT_ENTER);
    }

    @Override
    public void exitScope() {
        System.out.println("exitScope");

        Event event = eventList.get(currentEventIndex++);
        assertEquals(event.type, EXPECT_EXIT);
    }

    @Override
    public int newNet(String shortName, int cloneId, int width) {
        System.out.println("newNet " + shortName + " " + cloneId + " " + width);

        Event event = eventList.get(currentEventIndex++);
        assertEquals(event.type, EXPECT_NET);
        assertEquals(event.name, shortName);
        assertEquals(event.id, cloneId);
        assertEquals(event.width, width);

        return nextNetId++;
    }

    @Override
    public void appendTransition(int id, long timestamp, BitVector values) {
        System.out.println("appendTransition " + id + " " + timestamp + " " + values.toString(2));

        Event event = eventList.get(currentEventIndex++);
        assertEquals(event.type, EXPECT_TRANSITION);
        assertEquals(event.timestamp, timestamp);

        // Convert to string instead of using compare so Z and X values are
        // handled correctly.
        assertEquals(event.values.toString(2), values.toString(2));
    }

    @Override
    public void loadFinished() {
        System.out.println("loadFinished");

        Event event = eventList.get(currentEventIndex++);
        assertEquals(event.type, EXPECT_FINISHED);
    }

    void expectTimescale(int order) {
        Event event = new Event(EXPECT_TIMESCALE);
        event.timestamp = order;
        eventList.add(event);
    }

    void expectEnterScope(String name) {
        Event event = new Event(EXPECT_ENTER);
        event.name = name;
        eventList.add(event);
    }

    void expectExitScope() {
        eventList.add(new Event(EXPECT_EXIT));
    }

    void expectNewNet(String name, int cloneId, int width) {
        Event event = new Event(EXPECT_NET);
        event.name = name;
        event.id = cloneId;
        event.width = width;
        eventList.add(event);
    }

    void expectAppendTransition(int id, long timestamp, String bitString) {
        Event event = new Event(EXPECT_TRANSITION);
        event.id = id;
        event.timestamp = timestamp;
        event.values = new BitVector(bitString, 2);
        eventList.add(event);
    }

    void expectLoadFinished() {
        eventList.add(new Event(EXPECT_FINISHED));
    }
}