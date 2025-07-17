# BlockMiner

可以帮助破坏一些生存模式下不可破坏的方块的客户端mod

# 使用须知

BlockMiner 大量使用原版 Minecraft 客户端不可能做到的操作完成破坏方块，因此可能在绝大多数服务器内被视为外挂。  
请遵守服务器规则游玩，使用 BlockMiner 前先询问服务器管理员和其他玩家的意见。对于因使用 BlockMiner 造成财产损失和人员伤亡的，制作者不承担任何责任。

# 说明

BlockMiner 支持使用红石火把或拉杆这两种方案破坏方块。

#### 破坏方块默认白名单（可作为 BlockMiner 破坏方块的目标）：
- 基岩
#### 依赖方块默认白名单（可为能源方块提供支撑）：
- 粘液块
#### 需求：
必需： 活塞 * 2 + 红石火把或拉杆 * 1 + 效率 5 铁镐（或更高材质，仅对于红石火把方案）  
可选： 依赖方块默认白名单内方块 | 信标提供的急迫效果（挖活塞挖得更快）  
建议安装但可选模组： fabric-api（不装用不了命令、看不了i18n文本）

# 使用方法

空手右键点击破坏方块白名单内方块启动或关闭 BlockMiner  
启动后，对破坏方块白名单内方块左键点击， BlockMiner 会尝试破坏那个方块

## 命令列表

| 命令                                                             | 描述                                                                                                               |
|----------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| /blockminer toggle                                             | 切换BlockMiner的状态为开启/关闭                                                                                            |
| /blockminer config \<reload/save/reset>                        | 重新加载/保存/重置当前的设置                                                                                                  |
| /blockminer target-block whitelist                             | 显示目标方块白名单（即当前所有可指定为目标方块的方块名）                                                                                     |
| /blockminer target-block whitelist \<add/remove> \[block_name] | 添加/移除指定方块名到目标方块白名单                                                                                               |
| /blockminer depend-block whitelist                             | 显示依赖方块白名单（即当前所有可作为手动放置的依赖方块的方块名）                                                                                 |
| /blockminer depend-block whitelist \<add/remove> \[block_name] | 添加/移除指定方块名到依赖方块白名单                                                                                               |
| /blockminer area \<x> \<y> \<z> \<x2> \<y2> \<z2>              | 将坐标从(x, y, z)到(x2, y2, z2)中的所有可指定为破坏目标的方块指定为破坏目标，仅在设置中debug开启时可用                                                 |
| /blockminer blink-during-tasks-tick \[true/false]              | 显示或配置blink-during-tasks-tick的值（此模式下会在处理任务时暂存发送的数据包并在当前tick处理完毕后重新发送这些数据包）                                        |
| /blockminer debug \[true/false]                                | 显示或配置debug的值                                                                                                     |
| /blockminer distance-calculation-mode \[old/1.19/1.20.6]       | 显示或配置distance-calculation-mode的值（即距离计算模式，不同的MC服务器版本允许不同距离的最大手长，尝试调整此值以适配不同的服务器版本）                                |
| /blockminer headless-piston-mode \[true/false]                 | 显示或配置headless-piston-mode的值（即无头活塞模式，此模式下会改为建造一个指向目标方块的无头活塞）                                                      |
| /blockminer ping-spike-threshold \[0-1200]                     | 显示或配置ping-spike-threshold的值（即延迟尖峰阈值，如果它的值大于0，BlockMiner会在部分操作做完后等待\<ping-spike-threshold的值>个游戏刻，以保证服务器正确处理了这些操作） |

## 交流群

#### [\[QQ频道\]](https://pd.qq.com/s/ddfsvbi80)

#### [\[QQ群聊\]](https://qm.qq.com/q/bYIZVmxjPy)

## 致谢

### [LXYan2333](https://github.com/LXYan2333)

[Fabric-Bedrock-Miner](https://github.com/LXYan2333/Fabric-Bedrock-Miner) 项目让我看到 mod 破基岩的可行性 并在现在付诸行动

### [As_One_](https://space.bilibili.com/259168987)

[【我的世界】丐😍中😍丐破基🐔岩](https://www.bilibili.com/video/BV13e4y1m7s1) 视频提出了使用拉杆代替红石火把破基岩

### [Bunny_i](https://github.com/bunnyi116)

[Fabric-Bedrock-Miner 分支](https://github.com/bunnyi116/fabric-bedrock-miner)维护者 模组开发前和期间和我讨论了去信标破基岩的可行性 对我提供了技术上的帮助

### [Fallen_Breath](https://github.com/Fallen-Breath)

[fabric-mod-template](https://github.com/Fallen-Breath/fabric-mod-template)教会我如何配置[preprocessor](https://github.com/ReplayMod/preprocessor) 我没找到它的文档

### [Fabric](https://fabricmc.net)工具链开发者

\-
