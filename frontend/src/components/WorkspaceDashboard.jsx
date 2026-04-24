import { Button, Col, Progress, Row, Space, Tag, Typography } from "antd";
import {
  ArrowRightOutlined,
  CreditCardOutlined,
  DatabaseOutlined,
  LineChartOutlined,
  RocketOutlined
} from "@ant-design/icons";

const { Paragraph, Title, Text } = Typography;

function formatBytes(value) {
  if (!value) {
    return "0 B";
  }
  if (value < 1024) {
    return `${value} B`;
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${(value / (1024 * 1024)).toFixed(2)} MB`;
}

function MetricCard({ label, value, hint, icon }) {
  return (
    <div className="dashboard-metric-card">
      <div className="dashboard-metric-icon">{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{hint}</p>
    </div>
  );
}

export function WorkspaceDashboard({ user, recentJobs, jobs, health }) {
  const succeededJobs = recentJobs.filter((job) => job.status === "SUCCEEDED");
  const totalSavedBytes = succeededJobs.reduce((sum, job) => sum + (job.savedBytes || 0), 0);
  const monthlyUsagePercent = Math.min(100, Math.round(((20 - user.creditBalance) / 20) * 100));

  return (
    <div className="workspace-dashboard">
      <section className="workspace-welcome-card">
        <div>
          <Tag color="blue">Workspace Home</Tag>
          <Title level={2}>Welcome back, {user.email.split("@")[0]}</Title>
          <Paragraph>
            오늘 배치를 바로 시작하거나 최근 작업을 다시 열어보세요. 이 화면은 업로드 툴보다 운영 대시보드처럼 보이도록 재구성했습니다.
          </Paragraph>
        </div>
        <Space wrap size={[10, 10]}>
          <Button type="primary" size="large" href="#workspace-uploader" icon={<RocketOutlined />}>
            New Batch
          </Button>
          <Button size="large" href="#workspace-results" icon={<ArrowRightOutlined />}>
            View Results
          </Button>
        </Space>
      </section>

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12} xl={6}>
          <MetricCard
            label="Current Plan"
            value={user.plan}
            hint="업그레이드 흐름과 billing UX를 붙일 자리"
            icon={<CreditCardOutlined />}
          />
        </Col>
        <Col xs={24} md={12} xl={6}>
          <MetricCard
            label="Credits Left"
            value={user.creditBalance}
            hint="무료 체험 한도 기준"
            icon={<LineChartOutlined />}
          />
        </Col>
        <Col xs={24} md={12} xl={6}>
          <MetricCard
            label="Recent Jobs"
            value={recentJobs.length}
            hint="최근 워크스페이스 이력"
            icon={<DatabaseOutlined />}
          />
        </Col>
        <Col xs={24} md={12} xl={6}>
          <MetricCard
            label="Storage Saved"
            value={formatBytes(totalSavedBytes)}
            hint="최근 성공 작업 기준"
            icon={<RocketOutlined />}
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} className="workspace-summary-grid">
        <Col xs={24} xl={16}>
          <div className="dashboard-summary-card">
            <div className="dashboard-summary-head">
              <div>
                <Text strong>Usage Overview</Text>
                <Paragraph type="secondary">
                  무료 플랜 기준 사용량과 현재 처리 모드를 함께 보여줘서 SaaS 대시보드 느낌을 강화합니다.
                </Paragraph>
              </div>
              <Tag color={health.status === "online" ? "success" : health.status === "checking" ? "processing" : "error"}>
                {health.processingMode || "sync"} mode
              </Tag>
            </div>
            <div className="usage-track">
              <div className="usage-track-copy">
                <strong>Monthly Trial Usage</strong>
                <span>{20 - user.creditBalance} / 20 credits used</span>
              </div>
              <Progress percent={monthlyUsagePercent} strokeColor="#1677ff" showInfo={false} />
            </div>
            <div className="usage-pill-row">
              <span>{jobs.length > 0 ? `${jobs.length} jobs in current batch` : "No active batch yet"}</span>
              <span>{recentJobs.length} jobs in history</span>
              <span>{health.maxBatchSize || "-"} files per batch</span>
            </div>
          </div>
        </Col>
        <Col xs={24} xl={8}>
          <div className="dashboard-plan-card">
            <Tag color="gold">Upgrade Path</Tag>
            <strong>Move To Pro</strong>
            <p>배치 크기, 이력 보드, 우선 처리, 팀 공유가 필요한 순간을 보여주는 SaaS 업그레이드 영역입니다.</p>
            <ul className="dashboard-bullet-list">
              <li>Higher monthly limits</li>
              <li>Expanded job history</li>
              <li>Priority processing</li>
            </ul>
            <Button type="primary" block>
              Upgrade Plan
            </Button>
          </div>
        </Col>
      </Row>
    </div>
  );
}
