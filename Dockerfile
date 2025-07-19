# --- 第一阶段: 构建 ---
# 使用JDK 21 的镜像
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# 设置工作目录
WORKDIR /app


# 配置阿里云镜像
RUN mkdir -p /root/.m2 && \
    echo '<settings><mirrors><mirror><id>aliyunmaven</id><mirrorOf>*</mirrorOf><name>阿里云公共仓库</name><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml


COPY pom.xml .
RUN mvn dependency:go-offline

# 2. 复制所有源代码
COPY src ./src

# 3. 执行 Maven 打包命令, 并跳过 dockerfile 插件以避免循环构建
RUN mvn clean package -DskipTests -Ddocker.build.skip=true

# --- 第二阶段: 运行 ---
FROM eclipse-temurin:21-jre-jammy

# 设置工作目录
WORKDIR /app

# 安装 ffmpeg
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

# 从构建阶段复制打包好的 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 声明应用将使用的端口
EXPOSE 8081

# 定义容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]