[![Build Status](https://travis-ci.org/jbush001/WaveView.svg?branch=master)](https://travis-ci.org/jbush001/WaveView)

WaveView is a Java based waveform viewer.  It reads VCD (value change dump)
files.

# Setup
## MacOS

Install JDK from:

  http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

## Linux (Ubuntu)

	  sudo apt-get install openjdk-8-jdk

# Building

    ./gradlew build

# Running

    java -jar build/libs/WaveView.jar <trace file>

