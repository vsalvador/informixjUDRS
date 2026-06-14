PROJECT_NAME=PublishMQTT

SRC_DIR=src/main
BUILD_DIR=build
CLASSES_DIR=$(BUILD_DIR)/classes
DIST_DIR=$(BUILD_DIR)/dist
LIB_DIR=lib

JAR_FILE=$(DIST_DIR)/$(PROJECT_NAME).jar

INSTALL_DIR=$(INFORMIXDIR)/extend/krakatoa

JAVA_SOURCES := $(shell find $(SRC_DIR) -name "*.java")

build: $(JAR_FILE)

$(JAR_FILE): $(JAVA_SOURCES)
	@echo "Compiling Java sources..."
	javac \
		-cp "$(LIB_DIR)/*" \
		-d $(CLASSES_DIR) \
		$(JAVA_SOURCES)

	@echo "Creating JAR..."
	mkdir -p $(DIST_DIR)
	jar --create --file $(JAR_FILE) \
	    --manifest MANIFEST.MF \
	    -C $(CLASSES_DIR) .

install: build
	@if [ -z "$(INFORMIXDIR)" ]; then \
		echo "ERROR: INFORMIXDIR is not set"; \
		exit 1; \
	fi

	@echo "Installing to $(INSTALL_DIR)..."

	mkdir -p $(INSTALL_DIR)

	cp $(JAR_FILE) $(INSTALL_DIR)/
	cp $(LIB_DIR)/*.jar $(INSTALL_DIR)/

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


