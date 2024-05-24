JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = $(wildcard crafter/*.java)

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) crafter/*.class
