import { Button, Col, Row, Space, Statistic, Tag, Typography } from "antd";
import { ThunderboltOutlined, ScissorOutlined, CloudUploadOutlined } from "@ant-design/icons";

import { PanelCard } from "./ui/PanelCard";

const { Paragraph, Title, Text } = Typography;

function MetricTile({ title, value, suffix }) {
  return (
    <div className="metric-tile">
      <Statistic title={title} value={value} suffix={suffix} />
    </div>
  );
}

function serviceStatusTagColor(healthStatus) {
  if (healthStatus === "online") {
    return "success";
  }
  if (healthStatus === "checking") {
    return "processing";
  }
  return "error";
}

export function HeroSection({ user, onLogout, health }) {
  const backendStateLabel = health.status === "online"
    ? "Backend Online"
    : health.status === "checking"
      ? "Checking Backend"
      : "Backend Offline";

  return (
    <PanelCard
      className="hero-card workspace-hero-card"
      bodyStyle={{ padding: 24 }}
      title={(
        <Space size={12}>
          <Tag color="gold">Workspace</Tag>
          <Text type="secondary">Seller image optimization console</Text>
        </Space>
      )}
      extra={user ? (
        <Space>
          <Tag color="processing">{user.email}</Tag>
          <Button type="default" onClick={onLogout}>Log out</Button>
        </Space>
      ) : null}
    >
      <Row gutter={[24, 24]} align="middle">
        <Col xs={24} lg={15}>
          <Space direction="vertical" size={18} className="hero-copy-block">
            <Tag color="blue" className="hero-mini-tag">Operations Desk</Tag>
            <Title level={1} className="hero-title">
              현재 배치와 최근 작업 이력을 함께 보는 운영 워크스페이스
            </Title>
            <Paragraph className="hero-description">
              로그인 후에는 실제 앱처럼 업로드, 워터마크, 결과 비교, 다운로드와 작업 재확인 흐름이 한 화면에서 이어집니다.
            </Paragraph>
            <Space wrap size={[10, 10]}>
              <Tag color={serviceStatusTagColor(health.status)}>{backendStateLabel}</Tag>
              <Tag icon={<CloudUploadOutlined />} color="default">Batch + ZIP Upload</Tag>
              <Tag icon={<ScissorOutlined />} color="default">Manual Crop</Tag>
              <Tag icon={<ThunderboltOutlined />} color="default">Savings Review</Tag>
            </Space>
            <div className="hero-service-strip">
              <div className="service-chip">
                <span>API</span>
                <strong>{health.baseUrlLabel}</strong>
              </div>
              <div className="service-chip">
                <span>Mode</span>
                <strong>{health.processingMode || "-"}</strong>
              </div>
              <div className="service-chip">
                <span>Batch Limit</span>
                <strong>{health.maxBatchSize || "-"}</strong>
              </div>
              <div className="service-chip">
                <span>Last Check</span>
                <strong>{health.lastCheckedLabel}</strong>
              </div>
            </div>
          </Space>
        </Col>
        <Col xs={24} lg={9}>
          <div className="hero-metric-grid">
            <MetricTile title="Batch Limit" value={health.maxBatchSize || 10} suffix="files / run" />
            <MetricTile title="Workspace" value={user ? "Active" : "Login Required"} />
            <MetricTile title="Processing" value={health.processingMode || "Sync"} />
          </div>
        </Col>
      </Row>
    </PanelCard>
  );
}
