# 部署说明

本文档说明如何把揭棋网页服务运行在本机、临时公网地址或 Docker 环境中。

## 本机运行

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\web.ps1 8080
```

访问：

```text
http://127.0.0.1:8080/
```

## 局域网访问

启动 `web.ps1` 后，终端会输出局域网地址。让同一 Wi-Fi 或同一局域网内的设备打开该地址即可。

如果无法访问，优先检查：

- Windows 防火墙是否允许 Java 入站连接。
- 服务端电脑和访问设备是否在同一网络。
- 端口是否被其他程序占用。

## Cloudflare 临时公网演示

```powershell
cd D:\Code\java_big_work\Jieqi
powershell -ExecutionPolicy Bypass -File .\public-demo.ps1 8080
```

脚本会启动本地网页服务，并通过 Cloudflare Quick Tunnel 输出一个临时公网地址：

```text
https://xxxx.trycloudflare.com
```

注意：

- 这是临时地址，重启脚本后通常会变化。
- 你的电脑和终端窗口必须保持开启。
- 适合演示和测试，不建议作为长期正式服务。

## Docker

本地构建并运行：

```powershell
cd D:\Code\java_big_work\Jieqi
docker build -t jieqi .
docker run --rm -p 8080:8080 jieqi
```

后台运行：

```powershell
docker run -d --name jieqi -p 8080:8080 jieqi
```

停止服务：

```powershell
docker stop jieqi
docker rm jieqi
```

## 云服务器部署

在 Linux VPS 上可以使用 Docker：

```bash
git clone <your-repo-url>
cd Jieqi
docker build -t jieqi .
docker run -d --name jieqi -p 8080:8080 jieqi
```

然后在云服务器安全组和系统防火墙中开放 `8080` 端口。

## 数据持久化

棋谱会写入 `records/`。如果使用 Docker 并希望保留棋谱，可以挂载本地目录：

```powershell
docker run -d --name jieqi -p 8080:8080 -v ${PWD}\records:/app/records jieqi
```

如果部署在 Linux：

```bash
docker run -d --name jieqi -p 8080:8080 -v "$PWD/records:/app/records" jieqi
```
