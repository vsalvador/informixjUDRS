#!/usr/bin/make -f

PROJECT_NAME=Logs2Mqtt

SRC_DIR=src/main
BUILD_DIR=build
CLASSES_DIR=$(BUILD_DIR)/classes
DIST_DIR=$(BUILD_DIR)/dist
LIB_DIR=lib

JAR_FILE=$(DIST_DIR)/$(PROJECT_NAME).jar

INST_DIR=$(INFORMIXDIR)/extend/krakatoa

JAVA_SOURCES := $(shell find $(SRC_DIR) -name "*.java")

build: $(JAR_FILE)

$(JAR_FILE): $(JAVA_SOURCES) MANIFEST.MF
	@echo "Compiling Java sources..."
	javac \
		-cp "$(LIB_DIR)/*" \
		-d $(CLASSES_DIR) \
		$(JAVA_SOURCES)

	@echo "Creating JAR..."
	mkdir -p $(DIST_DIR)
	jar --create --file $(JAR_FILE) \
	    -C $(CLASSES_DIR) .

install: build
	@if [ ! -d "$(INST_DIR)" ]; then \
		echo "WARNING: $(INST_DIR) does not exists. Creating..."; \
		mkdir -p $(INST_DIR); \
	fi

	@echo "Installing to $(INST_DIR)..."
	cp $(JAR_FILE) $(INST_DIR)/
	@echo "Installation complete."

test:
	javac -cp "$(BUILD_DIR)/classes:$(CLASSPATH)" \
	      -d $(BUILD_DIR)/classes \
	      $$(find $(TEST_DIR) -name "*.java")
	java -cp "$(BUILD_DIR)/classes:$(CLASSPATH)" \
	     com.example.AppTest

clean:
	rm -rf $(BUILD_DIR)

.PHONY: build install clean


