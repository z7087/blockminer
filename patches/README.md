# AI 生成的补丁

`.patch` 文件放在这里，编译前由 `applyPatches.gradle` 自动应用（`git apply --3way`）。
补丁浮在源码之上，不需要实际 commit 或 merge。

```bash
./gradlew build      # 自动 apply patches/ 后编译
./gradlew applyPatches   # 只应用补丁
```
