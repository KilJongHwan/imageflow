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
      className="hero-card"
      bodyStyle={{ padding: 28 }}
      title={(
        <Space size={12}>
          <Tag color="gold">ImageFlow Commerce Ops</Tag>
          <Text type="secondary">Seller image optimization workspace</Text>
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
            <Tag color="blue" className="hero-mini-tag">Commerce Image Workspace</Tag>
            <Title level={1} className="hero-title">
              상품 이미지 업로드, 규격 조정, 결과 검수를 한 화면에서 처리하는 운영 워크스페이스
            </Title>
            <Paragraph className="hero-description">
              쿠팡, 네이버 스마트스토어, 컬리처럼 상품 이미지 작업이 반복되는 환경을 기준으로,
              대량 업로드부터 크롭, 압축, 결과 다운로드와 절감률 확인까지 이어서 볼 수 있게 구성했습니다.
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
