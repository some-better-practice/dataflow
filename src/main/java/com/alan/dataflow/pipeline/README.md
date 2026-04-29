## 模擬結果解讀
時序總覽

T+0.0s  3 jobs 同時觸發 (NATS file.detected)
        ├─ job-A [WAT/FAB12/N5]   light-1 開始 download
        ├─ job-B [SPC/FAB12/N5]   light-2 開始 download
        └─ job-C [WAT/FAB14/N3]   light-3 開始 download

T+1.0s  3 jobs 幾乎同時完成 download + compress

T+1.2s  ⏸  job-A 壓縮完 → 偵測到相依未滿足 → WAITING_DEPENDENCY (parking)
        job-B heavy-1 開始 format convert
        job-C heavy-2 開始 format convert

T+3.4s  job-B/C convert 完成 → 各自進 PROCESS
        job-B  SpcRollingAverageLogic (UCL/LCL ±3σ)
        job-C  Fab14N3WatRelaxedLogic (metal resistance normalization)

T+4.6s  job-B PROCESS 完成 → DependencyTracker.markCompleted()
        → [NATS] dependency.ready → job-A 解除 park
        → job-A 立刻進 heavy-1 做 format convert

T+5.5s  job-A convert 完成 → Fab12N5WatStrictLogic (sigma=3 strict)

T+9.1s  job-A DONE ✓  (checkpoint 顯示含 WAITING_DEPENDENCY 狀態)

怎麼讓不同 step 有不同 instance 數量（維持 monolith）
關鍵：同一個 JAR，靠 dataflow.worker.role 決定這個 pod 的行為

## 關鍵機制驗證
機制	驗證結果
Claim-check pattern	job 只存 storageRef 路徑，不持有資料本體
Worker pool 路由	download/compress/upload → light-*；convert/process → heavy-*
相依性 parking	job-A checkpoint 清楚看到 COMPRESSED → WAITING_DEPENDENCY → CONVERTING
Logic routing	同是 WAT，FAB12→Strict、FAB14→Relaxed，SPC→RollingAverage
Naming convention	壓縮後自動 rename: TSMC_FAB12_N5_WAT_WAT_N5_FAB12_20240101.dat.gz
MinIO/NFS 分區	convert 前先 copy 到 NFS WORKING zone，完成後結果到 MinIO PROCESSED
檔案結構

dataflow/src/main/java/com/tsmc/dataflow/
├── domain/         FileKey (5維複合鍵)、ProcessingJob、JobStatus
├── event/          FileDetectedEvent、StepCompletedEvent、DependencyReadyEvent
├── pipeline/       Orchestrator + 5個步驟服務 (各自跑在對應 worker pool)
├── logic/          Strategy interface + LogicRouter + 3個廠區實作
├── dependency/     DependencyTracker (相依規則註冊與 unblock)
├── storage/        StorageService (模擬 MinIO/NFS)
└── simulation/     SimulationRunner (場景入口)
要跑的話：JAVA_HOME=~/.sdkman/candidates/java/17.0.18-amzn mvn spring-boot:run

Monitor 任務逾時通知，不需要任何動作，模擬已在 35 秒前成功完成

## K8s Deployment（概念示意）

downloader  replicas: 3   --dataflow.worker.role=DOWNLOADER
            lightPool=16, heavyPool=1

converter   replicas: 5   --dataflow.worker.role=CONVERTER
            lightPool=2,  heavyPool=CPU核心數   ← 塞滿 CPU

processor   replicas: 8   --dataflow.worker.role=PROCESSOR
            lightPool=2,  heavyPool=CPU×2

uploader    replicas: 2   --dataflow.worker.role=UPLOADER
            lightPool=16, heavyPool=1
NATS queue group 做 competing consumers，同一個 subject 多個 pod 搶：


pipeline.convert.> → queue:"converter-group" → 5個 converter pod 其中一個處理
pipeline.process.> → queue:"processor-group" → 8個 processor pod 其中一個處理
好處：

JAR 只有一個，CI/CD 一條線
想加 converter 容量 → replicas: 5 → 10，不改程式碼
本機開發：role=ALL，一個 instance 跑全部
@OnWorkerRole 是預留的 condition annotation，未來若某些 Bean 只需要在特定 role 啟動（例如 converter pod 不需要 SimulationRunner），直接標上去就好：


@OnWorkerRole({WorkerRole.CONVERTER, WorkerRole.ALL})
@Service
public class FormatConversionService { ... }