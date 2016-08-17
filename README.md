WaveView is a Java based waveform viewer.  It reads VCD (value change dump)
files.

# Setup
## MacOS

Install JDK from:

  http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

Install Gradle (MacPorts https://www.macports.org/)

    sudo port install gradle

## Linux (Ubuntu)

	  sudo apt-get install openjdk-8-jdk gradle

# Building

    gradle build
    gradle test

# Running

    java -jar build/libs/WaveView.jar <trace file>

