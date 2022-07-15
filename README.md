# WaveView
[![CI](https://github.com/jbush001/WaveView/workflows/CI/badge.svg)](https://github.com/jbush001/WaveView/actions?query=workflow%3ACI)
[![codecov](https://codecov.io/gh/jbush001/WaveView/branch/master/graph/badge.svg)](https://codecov.io/gh/jbush001/WaveView)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/8d0a3f5b493d4f548706c2890566ebce)](https://www.codacy.com/app/jbush001/WaveView?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=jbush001/WaveView&amp;utm_campaign=Badge_Grade)

WaveView allows viewing waveform files produced by
hardware simulation tools like [Verilator](http://www.veripool.org/wiki/verilator)
and [Icarus Verilog](http://iverilog.icarus.com/).

![screenshot](https://raw.githubusercontent.com/wiki/jbush001/WaveView/screenshot.png)

## Development Setup
### MacOS

Install JDK from:

  <http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html>

### Linux (Ubuntu)

    sudo apt-get install openjdk-8-jdk

## Building

This project uses 'gradle' as its build system. The gradle wrapper and class
files are checked into this repository, so you don't need to install it
separately. It will download other dependencies automatically.

    ./gradlew build

This will run unit tests and the linter, which can take a while. To only create
a new JAR file:

    ./gradlew assemble

## Running

    java -jar build/libs/WaveView.jar [waveform file]

## Debugging Unit Test Failures

For Mockito failures, you can do enable verbose logging as follows:

At the top of the file, import the following:

    import static org.mockito.Mockito.withSettings;

Then modify the place where the mock is created to add verbose logging parameter, e.g.

    WaveformBuilder builder = mock(WaveformBuilder.class, withSettings().verboseLogging());
