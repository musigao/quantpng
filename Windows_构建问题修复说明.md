# Windows 构建问题修复说明

## 问题描述

在GitHub Actions中构建Windows DLL时出现以下错误：
```
LINK : fatal error LNK1181: cannot open input file '..\target\x86_64-pc-windows-msvc\release\libimagequant_sys.lib'
```

## 问题分析

这个错误有三个主要原因：

### 1. 路径错误
- **错误路径**: `..\target\x86_64-pc-windows-msvc\release\libimagequant_sys.lib`
- **正确路径**: `..\imagequant-sys\target\x86_64-pc-windows-msvc\release\imagequant_sys.lib`

### 2. 文件名错误
- **错误文件名**: `libimagequant_sys.lib`
- **正确文件名**: `imagequant_sys.lib`（根据 `imagequant-sys/Cargo.toml` 中的 `name = "imagequant_sys"`）

### 3. 构建顺序和工作目录问题
- 需要在 `imagequant-sys` 目录下构建 Rust 静态库
- 确保构建输出路径正确

## 解决方案

### 修复1: 更正构建命令
将：
```yaml
- name: Build Rust static library
  run: cargo build --release -p imagequant-sys --target ${{ matrix.target }}
```

改为：
```yaml
- name: Build Rust static library
  run: |
    cd imagequant-sys
    cargo build --release --target ${{ matrix.target }}
```

### 修复2: 更正链接库路径
将：
```cmd
link.exe /nologo /DLL /OUT:target/${{ matrix.lib_name }} ^
  target/jna_wrapper.obj ^
  ..\target\${{ matrix.target }}\release\libimagequant_sys.lib ^
  ws2_32.lib advapi32.lib userenv.lib
```

改为：
```cmd
link.exe /nologo /DLL /OUT:target/${{ matrix.lib_name }} ^
  target/jna_wrapper.obj ^
  ..\imagequant-sys\target\${{ matrix.target }}\release\imagequant_sys.lib ^
  ws2_32.lib advapi32.lib userenv.lib
```

### 修复3: 更新验证步骤
Windows验证：
```cmd
dir imagequant-sys\target\${{ matrix.target }}\release\
```

Linux验证：
```bash
ls -la imagequant-sys/target/${{ matrix.target }}/release/
```

## 文件名规律总结

根据 Cargo.toml 配置：
```toml
[lib]
name = "imagequant_sys"
crate-type = ["staticlib", "lib"]
```

生成的文件名为：
- **Linux**: `libimagequant_sys.a`
- **Windows**: `imagequant_sys.lib`
- **macOS**: `libimagequant_sys.a`

注意Windows上的静态库名称不包含 `lib` 前缀。

## 其他相关文件检查

确保以下文件中的路径也正确：
1. `.github/workflows/build-cross-platform.yml`
2. `.github/workflows/simple-build.yml`
3. `jna-wrapper/Makefile`
4. `jna-wrapper/CMakeLists.txt`

## 验证修复

修复后的完整构建流程：
1. 在 `imagequant-sys` 目录下构建 Rust 静态库
2. 验证生成的库文件存在
3. 使用正确的路径和文件名进行链接
4. 生成 Windows DLL

这样修复后，Windows构建应该能够成功找到并链接静态库文件。
