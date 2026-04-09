import { Col, Row, Tag, Typography } from "antd";

const { Paragraph } = Typography;

const presets = [
  {
    id: "naver",
    label: "Naver",
    title: "Smart Store Main",
    description: "정사각형 썸네일 중심의 검색형 상품 카드에 맞춘 설정입니다.",
    values: {
      presetId: "naver",
      width: "1200",
      height: "1200",
      quality: "82",
      aspectRatio: "1:1",
      cropMode: "center-crop",
      watermarkText: ""
    }
  },
  {
    id: "coupang",
    label: "Coupang",
    title: "Rocket-ready Hero",
    description: "쿠팡 대표 이미지에 맞춘 안정적인 정사각형 크롭과 압축 규칙입니다.",
    values: {
      presetId: "coupang",
      width: "1000",
      height: "1000",
      quality: "80",
      aspectRatio: "1:1",
      cropMode: "center-crop",
      watermarkText: ""
    }
  },
  {
    id: "kurly",
    label: "Kurly",
    title: "Editorial Product Cut",
    description: "세로감 있는 프리미엄 카탈로그형 레이아웃에 맞춘 4:5 출력입니다.",
    values: {
      presetId: "kurly",
      width: "1600",
      height: "2000",
      quality: "84",
      aspectRatio: "4:5",
      cropMode: "fit",
      watermarkText: ""
    }
  },
  {
    id: "custom",
    label: "Custom",
    title: "Manual Rule Set",
    description: "규격이 애매한 배치에 맞춰 직접 세부 규칙을 조정할 때 사용합니다.",
    values: {
      presetId: "custom",
      width: "1600",
      height: "",
      quality: "82",
      aspectRatio: "original",
      cropMode: "fit",
      watermarkText: ""
    }
  }
];

export function MarketplacePresetStrip({ activePresetId, onApplyPreset }) {
  return (
    <Row gutter={[12, 12]}>
      {presets.map((preset) => (
        <Col xs={24} md={12} key={preset.id}>
          <button
            className={preset.id === activePresetId ? "preset-option active" : "preset-option"}
            type="button"
            onClick={() => onApplyPreset(preset.values)}
          >
            <div className="preset-option-head">
              <Tag color={preset.id === activePresetId ? "blue" : "default"}>{preset.label}</Tag>
            </div>
            <strong>{preset.title}</strong>
            <Paragraph>{preset.description}</Paragraph>
          </button>
        </Col>
      ))}
    </Row>
  );
}
