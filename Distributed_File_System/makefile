JAR_PKG = MP3.jar

ENTRY_POINT = system.IntegratedSystem

RES_DIR = no

SOURCE_FILES = \
	Node.java \
	ActionNode.java \
	FileSystem.java\
    tools.java \
	PingHandler.java \
	PingReceiver.java \
	PingSender.java \
	StringSender.java \
	StringReceiver.java \
	Request.java \
	Listen.java \
	IntegratedSystem.java


JAVAC = javac

JFLAGS = -encoding UTF-8


vpath %.class system
vpath %.java src
# show help message by default
Default:
	@echo "make new: new project, create src, bin, res dirs."
	@echo "make build: build project."
	@echo "make clean: clear classes generated."
	@echo "make rebuild: rebuild project."
	@echo "make run: run your app."
	@echo "make jar: package your project into a executable jar."
build: $(SOURCE_FILES:.java=.class)
# pattern rule

%.class: %.java
	$(JAVAC) -cp bin -d bin $(JFLAGS) $<
rebuild: clean build
.PHONY: new clean run jar
new:
ifeq ($(RES_DIR),yes)
	mkdir -pv src bin res
else
	mkdir -pv src bin
endif
clean:
	rm -frv bin/*
run:
	java -cp bin $(ENTRY_POINT)
jar:
ifeq ($(RES_DIR),yes)
	jar cvfe $(JAR_PKG) $(ENTRY_POINT)  -C bin . res
else
	jar cvfe $(JAR_PKG) $(ENTRY_POINT) -C bin .
endif
