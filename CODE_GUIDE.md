# 代码完整使用说明与结构导览

本文档面向需要运行、阅读、维护或继续开发本项目的人，说明项目如何启动、各目录的作用、主要模块职责，以及关键文件之间的关系。

## 一、项目概览

本项目是一个揭棋对弈系统，核心由四部分组成：

- 规则引擎：负责棋盘、棋子、走法、吃子、将军、胜负判断。
- AI 引擎：负责人机对战中的搜索、评估、揭棋不确定性处理、普通象棋局面知识融合。
- Web 服务：提供浏览器对弈界面、房间、观战、棋谱列表和棋局回溯。
- 棋谱系统：记录每一局走子，支持前端回放和后续 AI 复盘分析。

代码以 Java 17 为主，不依赖复杂框架，便于直接阅读和调试。

## 二、快速运行

### 1. 启动网页对弈

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\web.ps1 8080
```

浏览器打开：

```text
http://127.0.0.1:8080/
```

### 2. 启动公网临时演示

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\public-demo.ps1 8080
```

终端会输出 `https://xxxx.trycloudflare.com` 形式的地址。把该地址发给别人即可远程对弈或观战。

### 3. 运行测试

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\test.ps1
```

测试覆盖规则引擎、JSON 编解码、棋谱记录和 AI 回归题库。

### 4. 命令行版本

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

常用命令：

```text
board
moves
move b2 b9
resign
help
quit
```

## 三、目录结构

```text
Jieqi/
├─ src/main/java/edu/jieqi/
│  ├─ Main.java
│  ├─ ai/
│  ├─ cli/
│  ├─ engine/
│  ├─ message/
│  ├─ model/
│  ├─ network/
│  ├─ record/
│  └─ web/
├─ src/test/java/edu/jieqi/
├─ config/
├─ puzzles/
├─ records/
├─ pom.xml
├─ Dockerfile
├─ README.md
├─ DEPLOYMENT.md
└─ CODE_GUIDE.md
```

## 四、核心模块说明

### 1. `model` 基础模型层

路径：

```text
src/main/java/edu/jieqi/model/
```

该目录定义项目中最基础的数据结构，其他模块都会使用它。

- `PlayerColor.java`：玩家颜色，区分红方和黑方。
- `PieceType.java`：棋子类型，例如帅/将、车、马、炮、兵、士、象等。
- `Position.java`：棋盘坐标，负责 x/y 坐标表达与转换。
- `Piece.java`：棋子对象，包含颜色、真实类型、是否已揭开等信息。
- `Move.java`：一步棋的起点和终点。
- `MoveResult.java`：走子结果，包含是否合法、是否吃子、是否翻开暗子、是否将军、是否结束等。

这一层应保持简单，不放复杂规则逻辑。

### 2. `engine` 规则引擎层

路径：

```text
src/main/java/edu/jieqi/engine/
```

这是项目最核心的规则部分。

- `Board.java`：棋盘数据结构，保存所有棋子的位置和初始暗子布局。
- `Game.java`：一局游戏的状态管理，负责当前回合、执行走子、认输、胜负状态等。
- `MoveGenerator.java`：生成某个局面下的合法走法。
- `RuleEngine.java`：判断具体走法是否合法，处理各类棋子的移动规则、吃子规则、将军判断等。
- `GameStatus.java`：游戏状态枚举，例如进行中、红胜、黑胜、和棋等。

如果要修改棋规，优先看 `RuleEngine.java` 和 `MoveGenerator.java`。如果要修改一局棋的生命周期，优先看 `Game.java`。

### 3. `ai` AI 策略层

路径：

```text
src/main/java/edu/jieqi/ai/
```

该目录负责人机对战 AI 的决策。

- `SearchAi.java`：AI 主体。负责候选走法生成、局面搜索、换子评估、攻防评估、暗子风险评估、长将惩罚、时间控制等。
- `AiConfig.java`：读取 `config/ai.properties`，把配置文件中的 AI 参数提供给搜索与评估逻辑。
- `XiangqiKnowledge.java`：普通象棋知识模块，用于明子较多时增强车、马、炮、兵、将杀和阵型判断。
- `ExperienceMemory.java`：经验数据结构和读写逻辑，主要用于历史策略数据。
- `ExperienceMerger.java`：合并经验文件。
- `SelfPlayTrainer.java`：自对弈训练入口。
- `XiangqiRecordTrainer.java`：从普通象棋坐标棋谱中导入明子局面知识。

当前 AI 更偏策略搜索型。它不是简单照搬历史棋谱，而是结合局面搜索、棋子价值、暗子概率和揭棋专用形状进行判断。

### 4. `web` 浏览器服务层

路径：

```text
src/main/java/edu/jieqi/web/
```

- `JieqiWebServer.java`：网页服务主入口。负责 HTTP 路由、前端页面输出、房间管理、人机对战、真人对战、观战、棋谱列表、棋局回溯、前端状态 JSON 等。

这个文件同时包含一部分前端 HTML/CSS/JavaScript 字符串。若要修改浏览器界面、按钮、棋盘交互、房间大厅、棋谱回放等，大多从这里开始。

### 5. `record` 棋谱记录层

路径：

```text
src/main/java/edu/jieqi/record/
```

- `GameRecord.java`：棋谱文件读写，负责将每步棋写入 `records/game-*.jsonl`。
- `RecordEntry.java`：单步棋谱记录结构，包括回合、颜色、起点、终点、吃子、翻出类型、时间戳、是否结束等。

前端棋谱列表和回溯功能依赖这里生成的记录。

### 6. `message` 消息协议层

路径：

```text
src/main/java/edu/jieqi/message/
```

用于命令行客户端/服务端或内部消息交互的 JSON 消息对象。

- `JsonCodec.java`：轻量 JSON 编解码。
- `MoveMessage.java`：走子请求。
- `MoveResultMessage.java`：走子结果响应。
- `GameStartMessage.java`：开局消息。
- `BoardMessage.java`：棋盘状态消息。
- `ErrorMessage.java`：错误消息。

### 7. `network` 本地联机层

路径：

```text
src/main/java/edu/jieqi/network/
```

- `JieqiServer.java`：Socket 服务端，支持两个命令行客户端联机。
- `JieqiClient.java`：Socket 客户端。

网页版本主要使用 `web/JieqiWebServer.java`，命令行双客户端联机使用该目录。

### 8. `cli` 命令行交互层

路径：

```text
src/main/java/edu/jieqi/cli/
```

- `CommandLineGame.java`：命令行对弈界面，处理 `board`、`moves`、`move`、`resign` 等命令。

### 9. `src/test` 测试层

路径：

```text
src/test/java/edu/jieqi/
```

- `RuleEngineSelfTest.java`：规则引擎自检。
- `JsonCodecSelfTest.java`：JSON 编解码自检。
- `GameRecordSelfTest.java`：棋谱记录自检。
- `AiRegressionSelfTest.java`：AI 回归题库自检，读取 `puzzles/ai-regression.tsv`。

修改规则或 AI 后，建议运行 `test.ps1`。

## 五、配置与数据文件

### `config/ai.properties`

AI 策略配置文件，包含搜索时间、关键局面时间、局面评估权重、长将惩罚、揭子倾向、攻击性、防守性等参数。

典型调整方向：

- 想让 AI 更快：降低搜索时间相关配置。
- 想让 AI 更谨慎：提高大子保护、被反吃风险、无效将军惩罚。
- 想让 AI 更进攻：提高推进、协同攻击、逼近对方将帅的奖励。

### `puzzles/ai-regression.tsv`

AI 回归题库。每一条题目用于约束某个已发现的问题，例如避免车换炮、避免无意义长将、避免暗子鲁莽互换等。

当发现 AI 犯了明确错误时，可以把局面加入这里，防止后续优化又把问题改回来。

### `records/`

运行时棋谱目录。项目会生成：

```text
records/game-时间.jsonl
```

这些文件默认不会提交到 GitHub，因为它们属于本地对局数据。仓库中只保留 `records/.gitkeep` 来保留目录。

## 六、脚本文件说明

- `web.ps1`：启动浏览器网页服务。
- `public-demo.ps1`：启动网页服务并开启 Cloudflare 临时公网隧道。
- `test.ps1`：编译并运行所有自检。
- `run.ps1`：启动命令行单机对弈。
- `server.ps1`：启动命令行联机服务端。
- `client.ps1`：启动命令行联机客户端。
- `train-ai.ps1`：运行自对弈训练。
- `merge-ai.ps1`：合并 AI 经验文件。
- `import-xiangqi.ps1`：导入普通象棋坐标棋谱。

## 七、关键运行流程

### 网页人机对战流程

1. 用户打开浏览器页面。
2. `JieqiWebServer` 创建或加入房间。
3. 用户点击棋子和目标点，前端发送走子请求。
4. 服务端调用 `Game` 和 `RuleEngine` 判断并执行走子。
5. 如果轮到 AI，服务端调用 `SearchAi.chooseMove(...)`。
6. AI 返回走法后，服务端执行该走法。
7. `GameRecord` 记录走子。
8. 前端刷新棋盘、消息、被吃棋子和合法落点高亮。

### AI 决策流程

1. 从当前 `Game` 生成合法候选走法。
2. 过滤或惩罚明显危险走法，例如白白送车、炮、马。
3. 结合揭棋规则评估暗子一次性身份、翻开收益和不确定性风险。
4. 对关键候选走法做多层搜索，预测自己和对手后续走法。
5. 使用普通象棋局面知识评估明子局面。
6. 综合吃子、保护、反吃、将军、将杀、长将、协同进攻、防守解危等分数。
7. 在配置允许的时间内返回最佳走法。

### 棋谱回溯流程

1. 每步合法走法写入 `records/game-*.jsonl`。
2. 前端棋谱列表读取可见记录。
3. 用户进入某一局回溯。
4. 服务端按棋谱重建棋盘变化。
5. 前端用播放/下一步方式展示历史走子。

## 八、常见开发入口

- 修改 AI 策略：`src/main/java/edu/jieqi/ai/SearchAi.java`
- 调 AI 参数：`config/ai.properties`
- 增加 AI 回归题：`puzzles/ai-regression.tsv`
- 修改棋规：`src/main/java/edu/jieqi/engine/RuleEngine.java`
- 修改合法走法生成：`src/main/java/edu/jieqi/engine/MoveGenerator.java`
- 修改网页界面：`src/main/java/edu/jieqi/web/JieqiWebServer.java`
- 修改棋谱记录：`src/main/java/edu/jieqi/record/GameRecord.java` 和 `RecordEntry.java`
- 修改命令行玩法：`src/main/java/edu/jieqi/cli/CommandLineGame.java`

## 九、GitHub 提交注意事项

默认 `.gitignore` 已忽略：

- `target/`
- `*.class`
- `records/game-*.jsonl`
- `records/ai-learning*.tsv`
- `records/ai-training.tsv`
- `records/hidden-records.txt`
- IDE、日志、临时文件

因此提交到 GitHub 的是项目源码、配置、测试题库和说明文档，不会把本地大量对局记录和训练数据公开。

## 十、建议开发流程

1. 修改代码或配置。
2. 运行测试：

```powershell
powershell -ExecutionPolicy Bypass -File .\test.ps1
```

3. 本地开网页实测：

```powershell
powershell -ExecutionPolicy Bypass -File .\web.ps1 8080
```

4. 如果是 AI 策略改动，至少用几局实战和 `puzzles/ai-regression.tsv` 回归题一起验证。
5. 确认没把 `records/game-*.jsonl`、`target/` 等运行产物加入提交。

