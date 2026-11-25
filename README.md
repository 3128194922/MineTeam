# MineTeam（本地增强版）

本项目是 [XiaoHuNao/MineTeam](https://github.com/XiaoHuNao/MineTeam) 的本地 Forge 1.20.1 分支。它保留原项目的队伍、驯服、队伍 PvP 与记分板队伍机制，并针对**玩家宠物、怪物友军和死亡后的队伍关系**做了集中修复。

## 与原项目的差异

- 新增 `TeamUtil`：统一处理队伍颜色、记分板队伍、宠物主人、驯服状态、狂暴状态和坐下状态。
- 新增 `TeamSync`：集中执行队伍创建、应用、同步、宠物队伍刷新，以及从记分板恢复队伍关系。
- 取消队伍专属 AI：不再注册集火/跟随 Goal；非白色队伍只通过 `Mob#canAttack` 过滤同队目标，
  白色队伍保持原版 AI。
- 宠物队伍跟随主人：驯服、玩家换队、复活、切换维度以及生物进入世界时，会同步宠物到主人的有效队伍颜色。
- 玩家死亡不再清空其宠物队伍：死亡事件不再直接移除队伍关系， revive/clone 时会保留队伍数据。
- 同队伍生物互认为友军：怪物不会把主人或同队生物作为攻击目标；同队生物之间的普通伤害会被取消。
- 最近受伤者数据仍会记录，但不再驱动队伍 AI；集火 Goal 已移除。
- 史莱姆分裂继承队伍：分裂出的新史莱姆会继承母体队伍并发光，便于维持宠物/队伍状态。
- 伤害规则细化：宠物主人与同队主人视为友军；只有宠物主人在潜行时才能伤害自己的宠物，
  其他同队玩家无法伤害该宠物；队伍 PvP 规则仍按双方开关判断。
- 颜色处理更稳健：队伍颜色统一小写校验，非法颜色回退为 `white`；界面图标也会使用规范化后的颜色。
- 网络同步更保守：C2S 只接受并应用当前玩家的颜色和 PvP 变更；S2C 会同步本地玩家自身，并按目标颜色加入或移出记分板队伍。
- 记分板变更回写：`ServerScoreboardMixin` 会在实体加入或离开记分板队伍时更新其驯服状态和队伍数据。
- 新增服务器启动队伍初始化：启动或创建出生点时预先创建有效颜色队伍，避免首次换队时找不到队伍。

相比上游 `forge/1.20.1` 分支，本地包含队伍、宠物与伤害判定等增强；具体差异以 `git diff` 为准。

## 环境要求

- Minecraft `1.20.1`
- Forge `47.3.10`（版本范围 `[47,)`）
- Java `17`
- Gradle Wrapper（项目已包含）
- 使用 Parchment 映射 `2023.09.03-1.20.1`

## 构建

Windows PowerShell：

```powershell
cd E:\Server_mod\MineTeam
.\gradlew.bat build
```

产物目录：`build/libs/`。

开发运行：

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
```

## 配置

配置文件：`config/mine_team.toml`。

- `allowDamageSelf`：是否允许同队实体互相攻击，默认 `true`。该值同时用于队伍的 `allowFriendlyFire` 设置。

驯服材料仍由 `config/mine_team.json5` 管理，沿用原项目行为。

## 主要代码入口

- `src/main/java/com/xiaohunao/mine_team/MineTeam.java`：Mod 入口、网络注册、配置注册。
- `src/main/java/com/xiaohunao/mine_team/common/capability/TeamCapability.java`：队伍数据、驯服交互、伤害判定。
- `src/main/java/com/xiaohunao/mine_team/common/util/TeamUtil.java`：本地新增的队伍与宠物关系工具。
- `src/main/java/com/xiaohunao/mine_team/common/team/TeamSync.java`：本地新增的队伍同步中心。
- `src/main/java/com/xiaohunao/mine_team/common/mixin/LivingEntityMixin.java`：非白色队伍 Mob 的
  `canAttack` 同队目标过滤。
- `src/main/java/com/xiaohunao/mine_team/common/mixin/MobMixin.java`：队伍数据；不再注册集火/跟随 Goal。
- `src/main/java/com/xiaohunao/mine_team/common/event/subscriber/`：登录、复活、换维度、攻击、驯服和实体入城事件。

## 已知边界

- 本 README 记录的差异以本地 `forge/1.20.1` 分支与上游同名分支的对比为准。
- 内容基于源码与提交差异整理；尚未通过实际客户端/服务器联机验证队伍目标过滤、伤害判定和网络同步表现。
- 原项目作者与许可保持不变，详见 `LICENSE`。
