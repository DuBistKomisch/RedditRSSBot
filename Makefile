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

# dependencies
${BIN}/RedditRSSBot.class: ${BIN}/TitleFormat.class ${BIN}/RedditCookieHandler.class

# how to make a .java into a .class
${CLASSES}: ${BIN}/%.class:${SRC}/%.java
	@mkdir -p ${BIN}
	${JC} ${JCARGS} -d ${BIN} ${SRC}/$*.java

# delete compiled files
clean:
	rm -rf ${BIN}

# print total line count (for funz)
lines:
	cat ${SOURCES} | wc -l
