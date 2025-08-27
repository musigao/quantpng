# Makefile for JNA wrapper of libimagequant
# 
# 使用方法:
#   make all           # 构建所有目标
#   make clean         # 清理生成的文件
#   make install       # 安装到系统目录
#   make test          # 运行测试

# 编译器设置
CC = gcc
CFLAGS = -std=c11 -Wall -Wextra -O3 -fPIC
LDFLAGS = -shared

# 平台特定设置
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
    # macOS
    LDFLAGS += -undefined dynamic_lookup
    LIB_EXT = .dylib
    CFLAGS += -fvisibility=hidden
else ifeq ($(UNAME_S),Linux)
    # Linux
    LIB_EXT = .so
    CFLAGS += -fvisibility=hidden
    LDFLAGS += -Wl,--version-script=exports.map
else
    # Windows (MinGW)
    LIB_EXT = .dll
    CFLAGS += -DBUILDING_JNA_WRAPPER
endif

# 目录设置
SRC_DIR = src
INCLUDE_DIR = include
BUILD_DIR = build
TARGET_DIR = target
TEST_DIR = test

# 查找静态库
IMAGEQUANT_LIB_PATHS = ../target/release ../imagequant-sys/target/release
IMAGEQUANT_LIB = $(word 1, $(wildcard $(addsuffix /libimagequant_sys.a, $(IMAGEQUANT_LIB_PATHS))))

ifeq ($(IMAGEQUANT_LIB),)
    $(error Could not find libimagequant_sys.a. Please run 'cargo build --release' first)
endif

# 输出库名称
OUTPUT_LIB = $(TARGET_DIR)/libimagequant_jna$(LIB_EXT)

# 源文件
SOURCES = $(SRC_DIR)/jna_wrapper.c
OBJECTS = $(BUILD_DIR)/jna_wrapper.o

# 包含路径
INCLUDES = -I$(INCLUDE_DIR) -I../imagequant-sys

# 默认目标
all: $(OUTPUT_LIB)

# 创建目录
$(BUILD_DIR):
	mkdir -p $(BUILD_DIR)

$(TARGET_DIR):
	mkdir -p $(TARGET_DIR)

# 编译对象文件
$(BUILD_DIR)/%.o: $(SRC_DIR)/%.c | $(BUILD_DIR)
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

# 链接动态库
$(OUTPUT_LIB): $(OBJECTS) | $(TARGET_DIR)
	$(CC) $(LDFLAGS) -o $@ $^ $(IMAGEQUANT_LIB)
	@echo "Built: $@"

# 清理
clean:
	rm -rf $(BUILD_DIR) $(TARGET_DIR)

# 测试
test: $(OUTPUT_LIB)
	@if [ -f $(TEST_DIR)/test_basic.c ]; then \
		$(CC) $(CFLAGS) $(INCLUDES) -o $(BUILD_DIR)/test_basic $(TEST_DIR)/test_basic.c -L$(TARGET_DIR) -limagequant_jna; \
		LD_LIBRARY_PATH=$(TARGET_DIR) $(BUILD_DIR)/test_basic; \
	else \
		echo "No test file found, skipping tests"; \
	fi

# 安装 (需要sudo权限)
install: $(OUTPUT_LIB)
	@echo "Installing library..."
	sudo cp $(OUTPUT_LIB) /usr/local/lib/
	sudo cp $(INCLUDE_DIR)/jna_wrapper.h /usr/local/include/
	@echo "Installation complete"

# 创建导出映射文件 (Linux)
exports.map:
	@echo "Creating symbol export map for Linux..."
	@echo "{ global: jna_*; local: *; };" > exports.map

# 显示库信息
info:
	@echo "Library path: $(OUTPUT_LIB)"
	@echo "Static lib: $(IMAGEQUANT_LIB)"
	@echo "Platform: $(UNAME_S)"
	@echo "Library extension: $(LIB_EXT)"

# 显示帮助
help:
	@echo "Available targets:"
	@echo "  all      - Build the JNA wrapper library"
	@echo "  clean    - Remove build artifacts"
	@echo "  test     - Build and run tests"
	@echo "  install  - Install library to system (requires sudo)"
	@echo "  info     - Show build information"
	@echo "  help     - Show this help message"

.PHONY: all clean test install info help

# Linux符号导出映射
ifneq ($(UNAME_S),Darwin)
ifneq ($(findstring MINGW,$(UNAME_S)),MINGW)
$(OUTPUT_LIB): exports.map
endif
endif
