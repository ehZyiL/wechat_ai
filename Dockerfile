# --- 第一阶段: 构建 ---
# 使用一个包含 Maven 和 JDK 21 的镜像
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# 设置工作目录
WORKDIR /app

# 1. 单独复制 pom.xml，利用 Docker 的层缓存机制
# 只要 pom.xml 没有变化，就不会重新下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. 复制所有源代码
COPY src ./src

# 3. 执行 Maven 打包命令, 并跳过 dockerfile 插件以避免循环构建
# 您这里的 -Ddocker.build.skip=true 是完全正确的
RUN mvn clean package -DskipTests -Ddocker.build.skip=true

# --- 第二阶段: 运行 ---
# 使用一个轻量的、且真实存在的 JRE 镜像作为最终的运行环境
# 【修正点】将 "focal" 修正为 "jammy"，因为 eclipse-temurin:21-jre-focal 镜像不存在
FROM eclipse-temurin:21-jre-jammy

# 设置工作目录
WORKDIR /app

# 【重要】安装 ffmpeg
# 您的 FormatFileService 依赖 ffmpeg 进行语音格式转换
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

# 从构建阶段复制打包好的 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 声明应用将使用的端口
EXPOSE 8081

# 定义容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]