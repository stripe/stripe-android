#!/bin/bash
set -eo pipefail

# Default to Java 25 if not specified
JAVA_VERSION=${JAVA_VERSION:-25}

# Determine architecture
ARCH=$(uname -m)
if [ "$ARCH" = "x86_64" ]; then
  ARCH="x64"
elif [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then
  ARCH="aarch64"
fi

# Determine OS (Adoptium uses "mac" for macOS, not "darwin")
OS=$(uname -s)
if [ "$OS" = "Darwin" ]; then
  OS="mac"
else
  OS=$(echo "$OS" | tr '[:upper:]' '[:lower:]')
fi

# Download Java from Adoptium (Eclipse Temurin)
JAVA_DIR="$HOME/java-${JAVA_VERSION}"

if [ ! -d "$JAVA_DIR" ]; then
  echo "Downloading Java ${JAVA_VERSION} for ${OS}-${ARCH}..."

  # Construct download URL for Adoptium
  DOWNLOAD_URL="https://api.adoptium.net/v3/binary/latest/${JAVA_VERSION}/ga/${OS}/${ARCH}/jdk/hotspot/normal/eclipse"

  mkdir -p "$JAVA_DIR"

  # Download (wrapped with retry.sh from bitrise.yml)
  curl -fSL --connect-timeout 30 --max-time 600 \
       "$DOWNLOAD_URL" -o "/tmp/java-${JAVA_VERSION}.tar.gz"

  # Verify download
  if [ ! -f "/tmp/java-${JAVA_VERSION}.tar.gz" ]; then
    echo "Downloaded file not found"
    exit 1
  fi

  # Extract to Java directory
  echo "Extracting Java to $JAVA_DIR..."
  if ! tar -xzf "/tmp/java-${JAVA_VERSION}.tar.gz" -C "$JAVA_DIR" --strip-components=1; then
    echo "Extraction failed, trying without strip-components..."
    rm -rf "$JAVA_DIR"
    mkdir -p "$JAVA_DIR"
    tar -xzf "/tmp/java-${JAVA_VERSION}.tar.gz" -C "$JAVA_DIR"

    # Find the actual JDK directory and move contents up
    JDK_DIR=$(find "$JAVA_DIR" -maxdepth 1 -type d -name "jdk*" -o -name "temurin*" | head -1)
    if [ -n "$JDK_DIR" ]; then
      echo "Moving contents from $JDK_DIR to $JAVA_DIR"
      mv "$JDK_DIR"/* "$JAVA_DIR"/
      rmdir "$JDK_DIR"
    fi
  fi

  rm "/tmp/java-${JAVA_VERSION}.tar.gz"

  # On macOS, the JDK is in Contents/Home subdirectory
  if [ -d "$JAVA_DIR/Contents/Home" ]; then
    JAVA_HOME="$JAVA_DIR/Contents/Home"
  else
    JAVA_HOME="$JAVA_DIR"
  fi

  # Verify Java was extracted correctly
  if [ ! -f "$JAVA_HOME/bin/java" ]; then
    echo "Java binary not found at $JAVA_HOME/bin/java. Directory contents:"
    ls -la "$JAVA_DIR"
    if [ -d "$JAVA_DIR/Contents" ]; then
      echo "Contents directory:"
      ls -la "$JAVA_DIR/Contents"
    fi
    exit 1
  fi
else
  echo "Java ${JAVA_VERSION} already installed at $JAVA_DIR"
  # On macOS, the JDK is in Contents/Home subdirectory
  if [ -d "$JAVA_DIR/Contents/Home" ]; then
    JAVA_HOME="$JAVA_DIR/Contents/Home"
  else
    JAVA_HOME="$JAVA_DIR"
  fi
fi

# Export for subsequent Bitrise steps
envman add --key JAVA_HOME --value "$JAVA_HOME"
envman add --key PATH --value "$JAVA_HOME/bin:$PATH"

# Verify
echo "Java installed successfully at $JAVA_HOME"
"$JAVA_HOME/bin/java" -version
