# 揭棋 Jieqi

这是一个 Java 17 编写的揭棋项目，支持命令行对弈、浏览器网页对弈、人机对战、真人房间、观战、棋局记录与回溯。项目重点在揭棋 AI：暗子较多时偏向揭子、试探与不确定性评估；明子较多时更多使用普通象棋的局面搜索、攻防、换子和将杀判断。

## 功能

- 人机对战：内置搜索型 AI，会结合揭棋暗子风险、车炮马价值、长将惩罚、进攻/防守布局等策略选点。
- 真人对战：创建房间后双方可通过浏览器进入，后续加入者可观战。
- 棋局回溯：对局结束或退出后保存棋谱，可在前端按时间查看与回放。
- 暗子公平性：正常对局中玩家和 AI 只知道自己已揭示的信息；回溯用于复盘，不参与对局决策。
- 局域网/公网演示：可在本机、局域网或 Cloudflare 临时隧道中运行。

## 环境要求

- JDK 17
- PowerShell
- 可选：Maven、Docker、Cloudflare Tunnel

如果使用脚本运行，项目会优先使用本机 JDK/Maven 环境。

## 快速启动网页版本

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\web.ps1 8080
```

启动后打开：

```text
http://127.0.0.1:8080/
```

同一局域网内的其他设备可以打开终端输出的局域网地址，例如：

```text
http://192.168.x.x:8080/
```

## 临时公网演示

适合把当前电脑上的服务临时分享给别人体验：

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\public-demo.ps1 8080
```

终端会输出一个类似下面的地址：

```text
https://xxxx.trycloudflare.com
```

把这个地址发给其他人即可。你的电脑和 PowerShell 窗口需要保持开启。

## 运行测试

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\test.ps1
```

测试会编译项目，并运行规则、记录、JSON、AI 回归题库等自检。

## 命令行版本

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

## 本地双客户端联机

先启动服务端：

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\server.ps1
```

再分别启动两个客户端：

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\client.ps1
```

如果客户端在另一台电脑上，使用服务端输出的 IP 和端口：

```powershell
powershell -ExecutionPolicy Bypass -File .\client.ps1 192.168.x.x 8887
```

## AI 配置

AI 参数集中在：

```text
config/ai.properties
```

常见配置包括搜索时间、局面评估权重、长将惩罚、揭子倾向、攻防策略等。修改后重启网页服务即可生效。

## 棋谱与回溯

服务运行时会在 `records/` 下生成棋谱文件：

```text
records/game-时间.jsonl
```

这些文件用于前端棋谱列表和回溯复盘。因为它们属于本地对局数据，默认不会提交到 GitHub。前端删除棋谱时需要密码：

```text
131415
```

删除操作只影响前端显示记录，不代表一定物理删除本地文件。

## 普通象棋棋谱导入

可以把普通象棋坐标棋谱导入为 AI 明子局面经验：

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\import-xiangqi.ps1 棋谱文件或目录
```

支持基础坐标格式，例如 `h2e2`、`h2-e2`、`move h2 e2`。中文记谱需要先转换成坐标格式。

## Docker 部署

```powershell
cd D:\Code\java_big_work\Jieqi
docker build -t jieqi .
docker run --rm -p 8080:8080 jieqi
```

浏览器打开：

```text
http://127.0.0.1:8080/
```

更多部署说明见 [DEPLOYMENT.md](DEPLOYMENT.md)。

## 目录结构

```text
src/main/java/edu/jieqi/     主程序、规则引擎、AI、网页服务、记录系统
src/test/java/edu/jieqi/     自检与回归测试
config/ai.properties         AI 策略配置
puzzles/ai-regression.tsv    AI 回归题库
records/                     本地棋谱与运行数据
```

更完整的代码使用说明、模块结构和各文件功能介绍见 [CODE_GUIDE.md](CODE_GUIDE.md)。

## GitHub 提交说明

仓库默认忽略：

- `target/` 编译产物
- `records/game-*.jsonl` 本地棋谱
- `records/ai-learning*.tsv` 和 `records/ai-training.tsv` 本地学习/训练数据
- IDE、日志、临时文件

这样可以把项目代码、配置和题库放到 GitHub，同时避免把本地大量对局数据一起公开。
