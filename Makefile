JC = javac
JX = java
SRC = src
BIN = bin
LIB = lib
CP = -cp ${BIN}:${LIB}:${LIB}/*
JCARGS = -g ${CP}
JXARGS = ${CP}

SOURCES = ${wildcard ${SRC}/*.java}
CLASSES = ${patsubst ${SRC}/%.java,${BIN}/%.class,${SOURCES}}

# compile all the files
all: ${CLASSES}

# how to make a .java into a .class
${CLASSES}: ${BIN}/%.class:${SRC}/%.java
	@mkdir -p ${BIN}
	${JC} ${JCARGS} -d ${BIN} ${SRC}/$*.java

# run the GUI
run: all
	${JX} ${JXARGS} HomestuckUpdateBot conf/homestuck_update_bot.conf

# delete compiled files
clean:
	rm -rf ${BIN}

# print total line count (for funz)
lines:
	cat ${SOURCES} | wc -l
