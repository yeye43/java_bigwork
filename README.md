# Jieqi 揭棋

一个 Java 17 编写的揭棋对弈项目，支持网页人机对战、真人房间、观战、棋谱记录与棋局回溯。AI 以策略搜索为主，会结合揭棋暗子不确定性、普通象棋明子局面、换子收益、长将惩罚、攻防布局等因素进行决策。

## 功能亮点

- 网页对弈：支持人机对战、真人对战、房间大厅和观战。
- 棋谱回溯：每局结束或退出后保存记录，可在前端回放复盘。
- 揭棋 AI：暗子多时偏向揭子和风险控制，明子多时加强普通象棋搜索与攻防判断。
- 公平信息：正常对局中玩家和 AI 不读取未揭暗子的真实身份，回溯数据只用于复盘。
- 易运行：提供 PowerShell 脚本、Dockerfile 和临时公网演示脚本。

## 快速开始

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\web.ps1 8080
```

浏览器打开：

```text
http://127.0.0.1:8080/
```

## 常用命令

| 用途 | 命令 |
| --- | --- |
| 启动网页服务 | `powershell -ExecutionPolicy Bypass -File .\web.ps1 8080` |
| 临时公网演示 | `powershell -ExecutionPolicy Bypass -File .\public-demo.ps1 8080` |
| 运行测试 | `powershell -ExecutionPolicy Bypass -File .\test.ps1` |
| 命令行单机对弈 | `powershell -ExecutionPolicy Bypass -File .\run.ps1` |
| 命令行联机服务端 | `powershell -ExecutionPolicy Bypass -File .\server.ps1` |
| 命令行联机客户端 | `powershell -ExecutionPolicy Bypass -File .\client.ps1` |

## 项目结构

```text
src/main/java/edu/jieqi/
├─ ai/        AI 搜索、评估、经验和棋谱导入
├─ cli/       命令行对弈入口
├─ engine/    棋盘、规则、合法走法、游戏状态
├─ message/   JSON 消息对象和编解码
├─ model/     棋子、坐标、走法等基础模型
├─ network/   命令行 Socket 联机
├─ record/    棋谱记录与读取
└─ web/       浏览器服务、房间、前端页面和回溯
```

其他重要目录：

```text
config/       AI 配置
puzzles/      AI 回归题库
records/      本地运行棋谱，不随仓库提交
docs/         代码导览和部署说明
```

## 文档

- [代码完整使用说明与结构导览](docs/CODE_GUIDE.md)
- [部署说明](docs/DEPLOYMENT.md)

## 测试

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\test.ps1
```

## Docker

```powershell
docker build -t jieqi .
docker run --rm -p 8080:8080 jieqi
```

## GitHub 提交说明

仓库默认忽略本地运行产物和隐私数据：

- `target/`
- `records/game-*.jsonl`
- `records/ai-learning*.tsv`
- `records/ai-training.tsv`
- `records/hidden-records.txt`

这样 GitHub 上只保留源码、配置、题库和文档，不上传本地大量对局记录。
