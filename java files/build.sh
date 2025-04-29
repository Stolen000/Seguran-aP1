#!/bin/bash

# Clean previous builds
echo "Cleaning old class files and JARs..."
rm -f *.class
rm -f mySharingClient.jar
rm -f mySharingServer.jar

# Compile Client files
echo "Compiling Client sources..."
javac privateFunctions.java wsPassLogic.java mySharingClient.java
if [ $? -ne 0 ]; then
  echo "Client compilation failed!"
  exit 1
fi

# Build Client JAR (include all class files)
echo "Building mySharingClient.jar..."
jar cfe mySharingClient.jar mySharingClient *.class

# Clean class files to avoid confusion
rm -f *.class

# Compile Server files
echo "Compiling Server sources..."
javac privateFunctions.java privateWsFunc.java macLogic.java mySharingServer.java
if [ $? -ne 0 ]; then
  echo "Server compilation failed!"
  exit 1
fi

# Build Server JAR (include all class files)
echo "Building mySharingServer.jar..."
jar cfe mySharingServer.jar mySharingServer *.class

# Done
echo "✅ Build finished! JAR files ready."
