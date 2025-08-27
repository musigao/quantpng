# Windows 链接库问题补充修复

## 新发现的问题

在修复了路径和文件名问题后，Windows构建出现了新的链接错误：

```
imagequant_sys.lib(std-xxx.rcgu.o) : error LNK2019: unresolved external symbol __imp_NtReadFile
imagequant_sys.lib(std-xxx.rcgu.o) : error LNK2019: unresolved external symbol __imp_RtlNtStatusToDosError
imagequant_sys.lib(std-xxx.rcgu.o) : error LNK2019: unresolved external symbol __imp_NtWriteFile
imagequant_sys.lib(std-xxx.rcgu.o) : error LNK2019: unresolved external symbol __imp_NtOpenFile
imagequant_sys.lib(std-xxx.rcgu.o) : error LNK2019: unresolved external symbol __imp_NtCreateNamedPipeFile
target\libimagequant_jna.dll : fatal error LNK1120: 5 unresolved externals
```

## 问题原因

Rust标准库在Windows上使用了NT API (Native API) 函数，这些函数位于 `ntdll.dll` 中。静态链接时需要显式指定 `ntdll.lib` 来解析这些符号。

## 解决方案

在所有Windows链接命令中添加必要的系统库：

### 原来的库列表：
```cmd
ws2_32.lib advapi32.lib userenv.lib
```

### 修复后的库列表：
```cmd
ws2_32.lib advapi32.lib userenv.lib ntdll.lib kernel32.lib
```

### 各库的作用：
- `ntdll.lib`: NT API函数 (NtReadFile, NtWriteFile, RtlNtStatusToDosError等)
- `kernel32.lib`: Windows核心API函数
- `ws2_32.lib`: Windows Socket API
- `advapi32.lib`: 高级Windows API (注册表、安全等)
- `userenv.lib`: 用户环境API

## 修复的文件

1. `.github/workflows/build-dynamic-libs.yml`
2. `.github/workflows/simple-build.yml`  
3. `.github/workflows/build-cross-platform.yml`

## 验证

修复后的链接命令应该能够成功解析所有符号，生成可用的Windows DLL文件。
