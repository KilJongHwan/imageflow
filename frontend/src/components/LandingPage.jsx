import { Button, Col, Collapse, Row, Space, Tag, Typography } from "antd";
import {
  ArrowRightOutlined,
  CheckCircleFilled,
  FolderOpenOutlined,
  LineChartOutlined,
  RocketOutlined
} from "@ant-design/icons";

import { AuthPanel } from "./AuthPanel";

const { Paragraph, Title, Text } = Typography;

const planItems = [
  {
    key: "free",
    name: "Starter",
    price: "Free",
    caption: "개인 셀러 테스트용",
    description: "상품 이미지 최적화를 가볍게 시작하고, 기본 워터마크와 수동 다운로드 흐름을 검증할 수 있습니다.",
    features: ["월 50장", "단일 워크스페이스", "기본 워터마크", "최근 작업 이력"],
    cta: "Get Started"
  },
  {
    key: "pro",
    name: "Pro",
    price: "$19",
    caption: "월간 운영 배치용",
    description: "반복 업로드, ZIP 배치 처리, 저장 용량 절감을 꾸준히 관리하는 셀러 팀을 위한 플랜입니다.",
    features: ["월 2,000장", "배치 ZIP 처리", "확장 작업 이력", "우선 처리 큐"],
    cta: "Start Pro Trial",
    featured: true
  },
  {
    key: "team",
    name: "Team",
    price: "$79",
    caption: "브랜드 운영팀용",
    description: "다수의 담당자가 같은 브랜드 자산 규칙과 이력 보드를 공유하며 운영하는 팀을 위한 구조입니다.",
    features: ["월 12,000장", "공유 워크스페이스", "역할별 접근", "운영 리포트"],
    cta: "Talk to Sales"
  }
];

const faqItems = [
  {
    key: "1",
    label: "ImageFlow는 어떤 팀을 위한 서비스인가요?",
    children: "상품 이미지 업로드와 리사이즈, 워터마크 적용, 배치 다운로드가 반복되는 셀러와 커머스 운영팀을 위한 서비스입니다."
  },
  {
    key: "2",
    label: "기존 편집툴과 무엇이 다른가요?",
    children: "편집 한 장보다 운영 배치에 초점을 둡니다. 규격 적용, ZIP 업로드, 결과 다운로드, 작업 이력 확인을 하나의 흐름으로 묶습니다."
  },
  {
    key: "3",
    label: "결제 연동은 실제로 붙어 있나요?",
    children: "현재 포트폴리오용 SaaS 흐름을 보여주기 위한 플랜 구조와 업그레이드 UX가 준비되어 있으며, 실제 결제 게이트웨이는 추후 연결 가능한 상태로 설계할 수 있습니다."
  }
];

function HealthRibbon({ health }) {
  const tone = health.status === "online" ? "success" : health.status === "checking" ? "processing" : "error";
  return (
    <Space wrap size={[10, 10]}>
      <Tag color={tone}>{health.summary}</Tag>
      <Tag color="default">Mode {health.processingMode || "sync"}</Tag>
      <Tag color="default">Batch {health.maxBatchSize || "-"}</Tag>
    </Space>
  );
}

export function LandingPage({ authProps, health }) {
  return (
    <div className="landing-shell">
      <section className="marketing-hero">
        <div className="marketing-nav">
          <div className="brand-lockup">
            <span className="brand-mark">ImageFlow</span>
            <span className="brand-sub">Commerce Image Ops</span>
          </div>
          <Space size={12}>
            <a href="#pricing" className="nav-link">Pricing</a>
            <a href="#faq" className="nav-link">FAQ</a>
            <Button type="primary" href="#get-started">Get Started</Button>
          </Space>
        </div>

        <Row gutter={[28, 28]} align="middle">
          <Col xs={24} xl={14}>
            <Space direction="vertical" size={18} className="landing-copy-stack">
              <Tag color="gold" className="landing-kicker">Seller Image Platform</Tag>
              <Title className="landing-title">
                상품 이미지를 더 빠르게 올리고, 더 가볍게 배포하고, 반복 작업은 줄이세요.
              </Title>
              <Paragraph className="landing-description">
                ImageFlow는 셀러와 커머스 운영팀을 위한 이미지 최적화 SaaS입니다.
                업로드, 리사이즈, 워터마크, 배치 다운로드, 작업 이력을 한 워크플로우 안에서 관리합니다.
              </Paragraph>
              <HealthRibbon health={health} />
              <Space wrap size={[12, 12]}>
                <Button type="primary" size="large" href="#get-started" icon={<RocketOutlined />}>
                  Get Started
                </Button>
                <Button size="large" href="#pricing">
                  See Pricing
                </Button>
              </Space>
              <div className="proof-grid">
                <div className="proof-card">
                  <strong>Up To 72%</strong>
                  <span>상품 이미지 평균 용량 절감</span>
                </div>
                <div className="proof-card">
                  <strong>Batch-Ready</strong>
                  <span>ZIP 업로드부터 결과 다운로드까지 한 흐름</span>
                </div>
                <div className="proof-card">
                  <strong>History First</strong>
                  <span>최근 작업과 결과 재확인을 워크스페이스에서 유지</span>
                </div>
              </div>
            </Space>
          </Col>

          <Col xs={24} xl={10}>
            <div className="hero-product-card">
              <div className="hero-product-screen">
                <div className="screen-topbar">
                  <span>Queue</span>
                  <span>Rules</span>
                  <span>Download</span>
                </div>
                <div className="screen-stat-stack">
                  <div className="screen-stat-card">
                    <FolderOpenOutlined />
                    <div>
                      <strong>Current Batch</strong>
                      <span>10 images / ZIP supported</span>
                    </div>
                  </div>
                  <div className="screen-stat-card">
                    <LineChartOutlined />
                    <div>
                      <strong>Savings Review</strong>
                      <span>source vs optimized result tracking</span>
                    </div>
                  </div>
                  <div className="screen-stat-card">
                    <CheckCircleFilled />
                    <div>
                      <strong>Workspace History</strong>
                      <span>reopen recent exports and downloads</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </Col>
        </Row>
      </section>

      <section className="landing-section">
        <div className="section-heading">
          <Tag color="blue">Why Teams Switch</Tag>
          <Title level={2}>이미지 편집툴이 아니라 운영 워크스페이스처럼 보이도록</Title>
          <Paragraph>
            ImageFlow는 한 장 편집보다 배치 운영과 자산 재사용에 집중합니다. 그래서 실제 서비스처럼 보이는 이유도 기능보다 흐름에 있습니다.
          </Paragraph>
        </div>

        <Row gutter={[18, 18]}>
          <Col xs={24} md={8}>
            <div className="value-card">
              <RocketOutlined />
              <strong>Faster Launches</strong>
              <span>상품 등록 전 리사이즈와 워터마크를 반복 작업 없이 한 번에 끝냅니다.</span>
            </div>
          </Col>
          <Col xs={24} md={8}>
            <div className="value-card">
              <FolderOpenOutlined />
              <strong>Operational Memory</strong>
              <span>최근 배치, 다운로드 이력, 절감률이 남아서 같은 작업을 다시 꺼내기 쉽습니다.</span>
            </div>
          </Col>
          <Col xs={24} md={8}>
            <div className="value-card">
              <LineChartOutlined />
              <strong>Cost Visibility</strong>
              <span>용량 절감과 처리량이 보이니 최적화가 감이 아니라 운영 지표가 됩니다.</span>
            </div>
          </Col>
        </Row>
      </section>

      <section className="landing-section use-case-grid">
        <div className="use-case-card">
          <span>01</span>
          <strong>신상품 등록 배치</strong>
          <p>촬영본 여러 장을 ZIP으로 올리고, 마켓 규격에 맞춘 뒤 바로 다운로드합니다.</p>
        </div>
        <div className="use-case-card">
          <span>02</span>
          <strong>브랜드 워터마크 적용</strong>
          <p>배치별로 선택적으로 워터마크를 켜고, 텍스트 또는 이미지 자산으로 합성합니다.</p>
        </div>
        <div className="use-case-card">
          <span>03</span>
          <strong>운영 이력 재확인</strong>
          <p>지난 배치의 결과와 절감률을 다시 열어보며 재작업 없이 빠르게 대응합니다.</p>
        </div>
      </section>

      <section id="pricing" className="landing-section pricing-section">
        <div className="section-heading">
          <Tag color="cyan">Pricing</Tag>
          <Title level={2}>작게 시작하고, 운영 규모에 맞춰 확장하세요</Title>
          <Paragraph>실제 SaaS처럼 보이도록 플랜 구조와 업그레이드 흐름을 준비했습니다.</Paragraph>
        </div>

        <Row gutter={[18, 18]}>
          {planItems.map((plan) => (
            <Col xs={24} lg={8} key={plan.key}>
              <div className={plan.featured ? "pricing-card featured" : "pricing-card"}>
                {plan.featured ? <Tag color="blue">Most Popular</Tag> : null}
                <strong>{plan.name}</strong>
                <div className="plan-price">{plan.price}<span>{plan.price === "Free" ? "" : "/mo"}</span></div>
                <p>{plan.caption}</p>
                <Paragraph>{plan.description}</Paragraph>
                <div className="plan-feature-list">
                  {plan.features.map((feature) => (
                    <div key={feature} className="plan-feature-item">
                      <CheckCircleFilled />
                      <span>{feature}</span>
                    </div>
                  ))}
                </div>
                <Button type={plan.featured ? "primary" : "default"} block size="large">
                  {plan.cta}
                </Button>
              </div>
            </Col>
          ))}
        </Row>
      </section>

      <section id="get-started" className="landing-section onboarding-section">
        <div className="section-heading">
          <Tag color="purple">Get Started</Tag>
          <Title level={2}>가입 후 바로 워크스페이스를 열고 첫 배치를 돌려보세요</Title>
          <Paragraph>
            랜딩은 서비스 설명을, 아래 카드는 실제 진입 폼을 담당합니다. 포트폴리오에서도 SaaS 구조가 분리돼 보이게 만듭니다.
          </Paragraph>
        </div>
        <div className="auth-panel-wrap">
          <AuthPanel {...authProps} />
        </div>
      </section>

      <section id="faq" className="landing-section faq-section">
        <div className="section-heading">
          <Tag color="geekblue">FAQ</Tag>
          <Title level={2}>실서비스처럼 보이게 하는 질문과 답변까지 같이</Title>
        </div>
        <Collapse items={faqItems} ghost className="faq-collapse" />
      </section>
    </div>
  );
}
