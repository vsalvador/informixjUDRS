#!/usr/bin/make -f

PROJECT_NAME=IfxLogger

SRC_DIR=src/main
BUILD_DIR=build
CLASSES_DIR=$(BUILD_DIR)/classes
DIST_DIR=$(BUILD_DIR)/dist
LIB_DIR=lib

JAR_FILE=$(DIST_DIR)/$(PROJECT_NAME).jar

INST_DIR=$(INFORMIXDIR)/extend/krakatoa
LIBS_DIR=$(INFORMIXDIR)/libjars

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
	    --manifest MANIFEST.MF \
            judr.properties \
	    -C $(CLASSES_DIR) .

install: build
	@if [ -z "$(INFORMIXDIR)" ]; then \
		echo "ERROR: INFORMIXDIR is not set"; \
		exit 1; \
	fi

	@if [ ! -d "$(INST_DIR)" ]; then \
		echo "WARNING: $(INST_DIR) does not exists. Creating..."; \
		mkdir -p $(INST_DIR); \
	fi

	@if [ ! -d "$(LIBS_DIR)" ]; then \
		echo "WARNING: $(LIBS_DIR) does not exists. Creating..."; \
		mkdir -p $(LIBS_DIR); \
	fi

	@echo "Installing to $(INST_DIR)..."
	cp $(JAR_FILE) $(INST_DIR)/
	@echo "Copying jar libraries to $(LIBS_DIR)..."
	cp $(LIB_DIR)/*.jar $(LIBS_DIR)/
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


