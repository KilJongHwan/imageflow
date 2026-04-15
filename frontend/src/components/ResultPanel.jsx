import { Alert, Button, Col, Descriptions, Empty, Image, List, Progress, Row, Space, Statistic, Tag, Typography } from "antd";
import { DownloadOutlined, ClockCircleOutlined, CheckCircleOutlined } from "@ant-design/icons";

import { PanelCard } from "./ui/PanelCard";

const { Paragraph, Text } = Typography;

function formatBytes(value) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }
  if (value < 1024) {
    return `${value} B`;
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${(value / (1024 * 1024)).toFixed(2)} MB`;
}

function formatPercent(value) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }
  return `${(value * 100).toFixed(1)}%`;
}

function formatDate(value) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

function JobList({ jobs, selectedJobId, onSelectJob, title }) {
  return (
    <div className="job-list-block">
      <div className="job-list-head">
        <Text strong>{title}</Text>
        <Tag>{jobs.length}</Tag>
      </div>
      <List
        size="small"
        dataSource={jobs}
        locale={{ emptyText: "표시할 작업이 없습니다." }}
        renderItem={(job) => (
          <List.Item>
            <button
              className={job.id === selectedJobId ? "job-list-item active" : "job-list-item"}
              type="button"
              onClick={() => onSelectJob(job.id)}
            >
              <div>
                <strong>{job.outputObjectKey?.split("/").pop() || job.id.slice(0, 8)}</strong>
                <span>{formatDate(job.createdAt)}</span>
              </div>
              <div className="job-list-item-meta">
                <Tag color={job.status === "SUCCEEDED" ? "success" : job.status === "FAILED" ? "error" : "processing"}>
                  {job.status}
                </Tag>
                <span>{formatPercent(job.reductionRate)}</span>
              </div>
            </button>
          </List.Item>
        )}
      />
    </div>
  );
}

export function ResultPanel({ jobs, recentJobs, selectedJobId, onDownloadBatch, onSelectJob, historyLoading }) {
  const currentBatch = jobs.length ? jobs : recentJobs;
  const selectedJob = currentBatch.find((job) => job.id === selectedJobId)
    || recentJobs.find((job) => job.id === selectedJobId)
    || jobs[0]
    || recentJobs[0]
    || null;
  const successfulCurrentJobs = jobs.filter((job) => job.status === "SUCCEEDED");
  const successfulRecentJobs = recentJobs.filter((job) => job.status === "SUCCEEDED");
  const totalSourceBytes = successfulRecentJobs.reduce((sum, job) => sum + (job.sourceFileSizeBytes || 0), 0);
  const totalResultBytes = successfulRecentJobs.reduce((sum, job) => sum + (job.resultFileSizeBytes || 0), 0);
  const totalSavedBytes = successfulRecentJobs.length ? totalSourceBytes - totalResultBytes : 0;
  const totalReductionRate = totalSourceBytes > 0 ? totalSavedBytes / totalSourceBytes : 0;
  const downloadTargetIds = successfulCurrentJobs.length
    ? successfulCurrentJobs.map((job) => job.id)
    : selectedJob?.status === "SUCCEEDED"
      ? [selectedJob.id]
      : [];

  return (
    <Space direction="vertical" size={18} className="full-width">
      <PanelCard
        className="workspace-card"
        title="Operations Desk"
        extra={downloadTargetIds.length > 0 ? (
          <Button icon={<DownloadOutlined />} onClick={() => onDownloadBatch(downloadTargetIds)}>
            Download {downloadTargetIds.length > 1 ? "Batch" : "Result"}
          </Button>
        ) : null}
      >
        <Paragraph className="panel-description">
          결과 검수, 용량 절감 확인, 최근 작업 재확인까지 한 곳에서 이어집니다. 지금 배치와 이전 히스토리를 따로 보지 않아도 됩니다.
        </Paragraph>
        {historyLoading ? (
          <Alert
            type="info"
            showIcon
            message="최근 작업 이력을 불러오는 중입니다."
            style={{ marginBottom: 16 }}
          />
        ) : null}

        <Row gutter={[16, 16]}>
          <Col xs={24} md={8}>
            <div className="metric-tile">
              <Statistic title="Recent History" value={recentJobs.length} prefix={<ClockCircleOutlined />} />
            </div>
          </Col>
          <Col xs={24} md={8}>
            <div className="metric-tile">
              <Statistic title="Current Batch" value={jobs.length} prefix={<CheckCircleOutlined />} />
            </div>
          </Col>
          <Col xs={24} md={8}>
            <div className="metric-tile">
              <Statistic title="Saved Bytes" value={formatBytes(totalSavedBytes)} />
              <Progress percent={Number((totalReductionRate * 100).toFixed(1))} strokeColor="#1677ff" />
            </div>
          </Col>
        </Row>
      </PanelCard>

      {selectedJob ? (
        <Space direction="vertical" size={18} className="full-width">
          <PanelCard className="workspace-card" title="Visual Review">
            <Row gutter={[16, 16]}>
              <Col xs={24} md={12}>
                <div className="compare-panel">
                  <Tag color="default">Source</Tag>
                  {selectedJob.sourceImageUrl ? (
                    <Image src={selectedJob.sourceImageUrl} alt="source asset" className="result-image" />
                  ) : (
                    <Empty description="원본 프리뷰 없음" />
                  )}
                </div>
              </Col>
              <Col xs={24} md={12}>
                <div className="compare-panel">
                  <Tag color="blue">Optimized</Tag>
                  {selectedJob.resultImageUrl ? (
                    <Image src={selectedJob.resultImageUrl} alt="optimized asset" className="result-image" />
                  ) : (
                    <Empty description="결과 프리뷰 대기 중" />
                  )}
                </div>
              </Col>
            </Row>

            <Descriptions column={2} className="job-descriptions">
              <Descriptions.Item label="Status">{selectedJob.status}</Descriptions.Item>
              <Descriptions.Item label="Created">{formatDate(selectedJob.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="Resolution">
                {selectedJob.targetHeight ? `${selectedJob.targetWidth} x ${selectedJob.targetHeight}` : selectedJob.targetWidth || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="Aspect Ratio">{selectedJob.aspectRatio || "-"}</Descriptions.Item>
              <Descriptions.Item label="Crop">{selectedJob.cropMode || "-"}</Descriptions.Item>
              <Descriptions.Item label="Quality">{selectedJob.quality || "-"}</Descriptions.Item>
              <Descriptions.Item label="Source Size">{formatBytes(selectedJob.sourceFileSizeBytes)}</Descriptions.Item>
              <Descriptions.Item label="Result Size">{formatBytes(selectedJob.resultFileSizeBytes)}</Descriptions.Item>
              <Descriptions.Item label="Saved">{formatBytes(selectedJob.savedBytes)}</Descriptions.Item>
              <Descriptions.Item label="Reduction">{formatPercent(selectedJob.reductionRate)}</Descriptions.Item>
              <Descriptions.Item label="Watermark">{selectedJob.watermarkText || "-"}</Descriptions.Item>
              <Descriptions.Item label="Watermark Style">{selectedJob.watermarkStyle || "-"}</Descriptions.Item>
              <Descriptions.Item label="Watermark Position">{selectedJob.watermarkPosition || "-"}</Descriptions.Item>
              <Descriptions.Item label="Output Key">{selectedJob.outputObjectKey || "-"}</Descriptions.Item>
            </Descriptions>

            {selectedJob.failureReason ? (
              <Alert type="error" showIcon message={selectedJob.failureReason} />
            ) : null}
          </PanelCard>

          <Row gutter={[18, 18]}>
            {jobs.length > 0 ? (
              <Col xs={24} xl={12}>
                <PanelCard className="workspace-card" title="Current Batch Queue">
                  <JobList jobs={jobs} selectedJobId={selectedJob.id} onSelectJob={onSelectJob} title="Current Batch" />
                </PanelCard>
              </Col>
            ) : null}

            <Col xs={24} xl={jobs.length > 0 ? 12 : 24}>
              <PanelCard className="workspace-card" title="Recent Workspace History">
                <JobList jobs={recentJobs} selectedJobId={selectedJob.id} onSelectJob={onSelectJob} title="History" />
              </PanelCard>
            </Col>
          </Row>
        </Space>
      ) : (
        <PanelCard className="workspace-card" title="Ready for Review">
          <Empty description="로그인 후 배치를 실행하면 최근 작업, 절감률, 전후 비교가 여기 표시됩니다." />
        </PanelCard>
      )}
    </Space>
  );
}
