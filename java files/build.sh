#!/bin/bash

echo "Cleaning old class files and JARs..."
rm -f *.class
rm -f mySharingClient.jar
rm -f mySharingServer.jar

echo "Compiling Client sources..."
javac privateFunctions.java wsPassLogic.java mySharingClient.java
if [ $? -ne 0 ]; then
  echo "Client compilation failed!"
  exit 1
fi

echo "Building mySharingClient.jar..."
jar cfe mySharingClient.jar mySharingClient *.class

rm -f *.class

echo "Compiling Server sources..."
javac privateFunctions.java privateWsFunc.java macLogic.java mySharingServer.java
if [ $? -ne 0 ]; then
  echo "Server compilation failed!"
  exit 1
fi

echo "Building mySharingServer.jar..."
jar cfe mySharingServer.jar mySharingServer *.class

# Done
echo "Build finished! JAR files ready."
