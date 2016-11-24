[![Build Status](https://travis-ci.org/jbush001/WaveView.svg?branch=master)](https://travis-ci.org/jbush001/WaveView)
[![codecov](https://codecov.io/gh/jbush001/WaveView/branch/master/graph/badge.svg)](https://codecov.io/gh/jbush001/WaveView)

WaveView allows viewing waveform files produced by
hardware simulation tools like [Verilator](http://www.veripool.org/wiki/verilator)
and [Icarus Verilog](http://iverilog.icarus.com/).

![screenshot](https://raw.githubusercontent.com/wiki/jbush001/WaveView/screenshot.png)

# Development Setup
## MacOS

Install JDK from:

  http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

## Linux (Ubuntu)

    sudo apt-get install openjdk-8-jdk

# Building

This project uses 'gradle' as its build system. The gradle wrapper and class
files are checked into this repository, so you don't need to install it
separately. It will download other dependencies automatically.

    ./gradlew build

This will run unit tests and the linter, which can take a while. To only create
a new JAR file:

    ./gradlew assemble

# Running

    java -jar build/libs/WaveView.jar [waveform file]
