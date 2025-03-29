# GOATED video this workflow is inspired from: https://www.youtube.com/watch?v=uuN5qkNgrkY

#JAVA_HOME = /Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home
JAVA_HOME = $(shell /usr/libexec/java_home)
CONTROLLER = src/com/supasulley/macrophone/Macrophone.java
LIB_FILE = src/main/resources/macrophone.dylib
NATIVE_DIR = src/main/native

# Build library by default
#all:
#	echo "turned off for dev"
all: clean buildLib

# Generate JNI header
header:
	mkdir -p ${NATIVE_DIR}
	javac -h ${NATIVE_DIR} src/main/java/com/supasulley/macrophone/*.java

# Generate signature for a particular class
sig:
	@bash -c 'read -p "Class name: " CLASSNAME && javap -s $$CLASSNAME'

# Generate signatures for our class
signatures:
	javac ${CONTROLLER} -d bin
	javap -s -p bin/com/supasulley/macrophone/Macrophone.class

# Build library
buildLib:
	clang++ -std=c++17 -framework Cocoa -framework AVFoundation -framework Foundation -framework Speech -fvisibility=hidden -Wall -shared -o ${LIB_FILE} ${NATIVE_DIR}/*.cpp ${NATIVE_DIR}/*.mm -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin
	# Compile C++ and Objective-C++ files (.cpp and .mm) into object files
#	clang++ -std=c++17 -Wall -fPIC -fvisibility=hidden -framework Cocoa -framework AVFoundation -framework CoreFoundation \
#		-c $(NATIVE_DIR)/*.cpp $(NATIVE_DIR)/*.mm -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin
#
#	# Compile Objective-C files (.m) into object files
#	clang -Wall -fPIC -fvisibility=hidden -framework Speech -framework Cocoa -framework AVFoundation -framework CoreFoundation \
#		-c $(NATIVE_DIR)/*.m -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin
#
#	# Link all object files together into a shared library (.dylib)
#	clang++ -shared -o $(LIB_FILE) $(NATIVE_DIR)/*.o -framework Speech -framework Cocoa -framework AVFoundation -framework CoreFoundation
	# Sanity checks
	file ${LIB_FILE}
	nm ${LIB_FILE} | grep Java

# Remove built library
clean:
	rm -f ${LIB_FILE}
